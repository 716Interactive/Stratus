package com.slamstudios.stratus.services

import com.slamstudios.stratus.config.AppConfig
import com.slamstudios.stratus.db.schema.ServerState
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PteroTemplateConfig(
    val memory: Int = 2048,
    val disk: Int = 5120,
    val cpu: Int = 0,
    val startup: String = "java -jar server.jar",
    val image: String = "ghcr.io/pterodactyl/yolks:java_17",
    val env: Map<String, String> = emptyMap()
)

object OrchestratorService {
    private val logger = LoggerFactory.getLogger(OrchestratorService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var running = false
    private lateinit var config: AppConfig

    fun start(config: AppConfig) {
        if (running) return
        this.config = config
        running = true
        logger.info("Stratus Orchestrator starting…")

        scope.launch {
            // Initial node sync on startup
            syncNodes()

            while (running) {
                try {
                    watchdogLogic()
                } catch (e: Exception) {
                    logger.error("Error in watchdog loop", e)
                }
                delay(30_000)
            }
        }

        startAutoscaler()
        
        logger.info("Stratus Orchestrator background loops started.")
    }

    private suspend fun syncNodes() {
        try {
            logger.info("Syncing nodes from Pterodactyl...")
            val pteroNodes = PterodactylService.getNodes()
            if (pteroNodes.isEmpty()) {
                logger.warn("No nodes returned from Pterodactyl or failed to fetch nodes.")
                return
            }

            for (pteroNode in pteroNodes) {
                val existing = NodeService.getByPterodactylId(pteroNode.id)
                val pteroAllocatedMem = pteroNode.allocated_resources?.memory ?: 0
                val pteroAllocatedDisk = pteroNode.allocated_resources?.disk ?: 0

                if (existing != null) {
                    if (existing.name != pteroNode.name ||
                        existing.totalMemory != pteroNode.memory ||
                        existing.totalDisk != pteroNode.disk ||
                        existing.host != pteroNode.fqdn ||
                        existing.allocatedMemory != pteroAllocatedMem ||
                        existing.allocatedDisk != pteroAllocatedDisk
                    ) {
                        logger.info("Auto-sync: Updating node ${existing.name} (Ptero ID: ${pteroNode.id}) -> RAM=${pteroNode.memory}MB, Disk=${pteroNode.disk}MB, AllocatedRAM=${pteroAllocatedMem}MB, AllocatedDisk=${pteroAllocatedDisk}MB")
                        NodeService.update(
                            id = existing.id,
                            name = pteroNode.name,
                            memory = pteroNode.memory,
                            disk = pteroNode.disk,
                            allocatedMemory = pteroAllocatedMem,
                            allocatedDisk = pteroAllocatedDisk
                        )
                    }
                } else {
                    logger.info("Auto-sync: Registering new Pterodactyl node '${pteroNode.name}' (Ptero ID: ${pteroNode.id}) -> RAM=${pteroNode.memory}MB, Disk=${pteroNode.disk}MB, AllocatedRAM=${pteroAllocatedMem}MB, AllocatedDisk=${pteroAllocatedDisk}MB")
                    NodeService.create(
                        Node(
                            id = UUID.randomUUID().toString(),
                            pterodactylId = pteroNode.id,
                            name = pteroNode.name,
                            host = pteroNode.fqdn,
                            token = "",
                            totalMemory = pteroNode.memory,
                            totalDisk = pteroNode.disk,
                            allocatedMemory = pteroAllocatedMem,
                            allocatedDisk = pteroAllocatedDisk
                        )
                    )
                }
            }
            logger.info("Successfully synced ${pteroNodes.size} node(s) from Pterodactyl.")
        } catch (e: Exception) {
            logger.error("Failed to auto-sync nodes from Pterodactyl", e)
        }
    }

    private fun startAutoscaler() {
        scope.launch {
            while (running) {
                try {
                    // Sync nodes dynamically prior to auto-scaling
                    syncNodes()
                    
                    autoscaleLogic()
                    
                    // Also check for backups
                    val config = BackupService.getConfig()
                    if (config?.lastBackupAt == null || LocalDateTime.parse(config.lastBackupAt).plusMinutes(config.backupIntervalMinutes.toLong()).isBefore(LocalDateTime.now())) {
                        BackupService.performBackup()
                    }

                } catch (e: Exception) {
                    logger.error("Error in autoscaler loop: ${e.message}")
                    // AuditService.error("SYSTEM", "Autoscaler loop crash: ${e.message}")
                }
                delay(30_000)
            }
        }
    }

    private suspend fun watchdogLogic() {
        val now = LocalDateTime.now()
        val startingTimeout = now.minusMinutes(5)
        val heartbeatTimeout = now.minusMinutes(2)

        val stuck = ServerService.getStuckServers(startingTimeout, heartbeatTimeout)
        if (stuck.isEmpty()) return

        logger.warn("Watchdog found ${stuck.size} stuck server(s).")
        for (server in stuck) {
            logger.info("Watchdog terminating stuck server ${server.id} (state: ${server.state}, lastHeartbeat: ${server.lastHeartbeat})")
            
            if (server.pterodactylId != null) {
                PterodactylService.deleteServer(server.pterodactylId)
            }
            ServerService.updateState(server.id, ServerState.TERMINATED)
        }
    }

    private suspend fun autoscaleLogic() {
        val groups = GroupService.getAll()
        for (group in groups) {
            val template = TemplateService.getById(group.templateId) ?: continue
            val servers = ServerService.getAll(group.id)
            val active = servers.filter { it.state in ServerState.ACTIVE }
            val ready = servers.filter { it.state == ServerState.READY }
            
            if (active.size < group.minServers) {
                val toCreate = group.minServers - active.size
                logger.info("Group ${group.name} is below min_servers (${active.size}/${group.minServers}). Creating $toCreate server(s).")
                repeat(toCreate) { provisionServer(group) }
                continue
            }

            if (ready.size < group.targetFreeSlots && active.size < group.maxServers) {
                val toCreate = (group.targetFreeSlots - ready.size).coerceAtMost(group.maxServers - active.size)
                if (toCreate > 0) {
                    logger.info("Group ${group.name} needs more free slots (${ready.size}/${group.targetFreeSlots}). Creating $toCreate server(s).")
                    repeat(toCreate) { provisionServer(group) }
                }
            }

            val empty = servers.filter { it.state == ServerState.EMPTY }
            if (active.size > group.minServers && empty.isNotEmpty()) {
                val cooldown = group.scaleDownCooldownSeconds
                val now = LocalDateTime.now()

                for (server in empty) {
                    val changedAt = LocalDateTime.parse(server.stateChangedAt)
                    val secondsEmpty = java.time.Duration.between(changedAt, now).seconds
                    
                    if (secondsEmpty >= cooldown) {
                        logger.info("Group ${group.name} has excess EMPTY server ${server.id} (empty for ${secondsEmpty}s). Draining.")
                        ServerService.updateState(server.id, ServerState.DRAINING)
                        if (server.pterodactylId != null) {
                            PterodactylService.deleteServer(server.pterodactylId)
                        }
                        ServerService.updateState(server.id, ServerState.TERMINATED)
                    } else {
                        logger.debug("Group ${group.name} has excess EMPTY server ${server.id}, but it is still in cooldown (${secondsEmpty}/${cooldown}s).")
                    }
                }
            }

            val currentVersionId = template.currentVersionId
            if (currentVersionId != null) {
                val outdated = active.filter { it.templateVersionId != currentVersionId }
                for (server in outdated) {
                    if (server.state == ServerState.READY || server.state == ServerState.EMPTY) {
                        logger.info("Server ${server.id} is running outdated version. Marking for drainage.")
                        ServerService.updateState(server.id, ServerState.DRAINING)
                        if (server.pterodactylId != null) {
                            PterodactylService.deleteServer(server.pterodactylId)
                        }
                        ServerService.updateState(server.id, ServerState.TERMINATED)
                    }
                }
            }
        }
    }

    private fun findAvailableNode(requiredMemory: Int, requiredDisk: Int, preferredNodeId: String? = null, strategy: String = "SPREAD"): Node? {
        val nodes = NodeService.getAll()
        
        // 1. Check preferred node first if specified
        if (preferredNodeId != null) {
            val preferred = nodes.find { it.id == preferredNodeId }
            if (preferred != null && preferred.canFit(requiredMemory, requiredDisk)) {
                return preferred
            }
        }

        // 2. Filter nodes that can fit the requirements
        val candidateNodes = nodes.filter { it.canFit(requiredMemory, requiredDisk) }
        
        if (candidateNodes.isEmpty()) return null

        // 3. Apply scheduling strategy
        return when (strategy.uppercase()) {
            "BIN_PACKING" -> {
                // Pick node with LEAST available memory to "fill" it up
                candidateNodes.minByOrNull { it.totalMemory - it.usedMemory() }
            }
            "SPREAD" -> {
                // Pick node with MOST available memory to balance load
                candidateNodes.maxByOrNull { it.totalMemory - it.usedMemory() }
            }
            else -> candidateNodes.random()
        }
    }

    private suspend fun provisionServer(group: ServerGroup) {
        val template = TemplateService.getById(group.templateId) ?: return
        val versionId = template.currentVersionId ?: return
        val version = TemplateService.getVersions(group.templateId).find { it.id == versionId } ?: return

        // Parse config from version
        val templateConfig = try {
            Json { ignoreUnknownKeys = true }.decodeFromString<PteroTemplateConfig>(version.configJson ?: "{}")
        } catch (e: Exception) {
            logger.error("Failed to parse configJson for version ${version.id}, using defaults", e)
            PteroTemplateConfig()
        }

        val node = findAvailableNode(templateConfig.memory, templateConfig.disk, group.preferredNodeId, group.schedulingStrategy)
        if (node == null) {
            logger.error("No nodes available for group ${group.name} (Requires ${templateConfig.memory}MB RAM)")
            return
        }

        val internalId = UUID.randomUUID().toString()
        val environment = mutableMapOf(
            "STRATUS_URL" to config.pterodactyl.orchestratorUrl,
            "STRATUS_TOKEN" to config.token,
            "STRATUS_SERVER_ID" to internalId,
            "STRATUS_GROUP" to group.name
        )
        // Add custom env from template
        environment.putAll(templateConfig.env)

        val pteroServer = PterodactylService.createServer(
            name = "${group.name}-${internalId.take(8)}",
            userId = config.pterodactyl.ownerId,
            eggId = version.eggId,
            nodeId = node.pterodactylId,
            memoryMb = templateConfig.memory,
            diskMb = templateConfig.disk,
            startup = templateConfig.startup,
            image = templateConfig.image,
            environment = environment
        )

        if (pteroServer != null) {
            ServerService.createWithId(
                id = internalId,
                pteroId = pteroServer.id,
                nodeId = node.id,
                groupId = group.id,
                templateVersionId = version.id,
                host = pteroServer.host ?: "127.0.0.1",
                port = pteroServer.port ?: 25565,
                memory = templateConfig.memory,
                disk = templateConfig.disk
            )

            // Asynchronously wait for Pterodactyl server to install, then synchronize files, then start the server
            scope.launch {
                logger.info("Server ${pteroServer.name} (Ptero ID: ${pteroServer.id}) created. Waiting for installation...")
                // Poll for installation status (up to 5 minutes)
                var installed = false
                val startPoll = LocalDateTime.now()
                while (!installed && LocalDateTime.now().isBefore(startPoll.plusMinutes(5))) {
                    delay(5000)
                    if (PterodactylService.isServerInstalled(pteroServer.id)) {
                        installed = true
                    }
                }
                
                if (installed) {
                    logger.info("Server ${pteroServer.name} is installed. Killing server to guarantee it is offline before cloning files...")
                    PterodactylService.sendPowerSignal(pteroServer.identifier, "kill")
                    delay(2000) // Allow time to process signal
                    
                    // Clear & clone the template files locally
                    val templateDir = File(template.localPath, template.id)
                    val volumeDir = File("/var/lib/pterodactyl/volumes/${pteroServer.uuid}")
                    
                    try {
                        if (volumeDir.exists()) {
                            logger.info("Synchronizing files from template directory ${templateDir.absolutePath} to ${volumeDir.absolutePath}...")
                            volumeDir.deleteRecursively()
                            volumeDir.mkdirs()
                            if (templateDir.exists()) {
                                templateDir.copyRecursively(volumeDir, overwrite = true)
                                logger.info("Template files synchronized successfully for ${pteroServer.name}.")
                            } else {
                                logger.warn("Template directory ${templateDir.absolutePath} does not exist. Starting server with default files.")
                            }
                        } else {
                            logger.error("Pterodactyl server volume not found at ${volumeDir.absolutePath}. Cannot clone template files locally.")
                        }
                    } catch (e: Exception) {
                        logger.error("Error synchronizing files for ${pteroServer.name}: ${e.message}", e)
                    }
                    
                    logger.info("Starting server ${pteroServer.name} (Identifier: ${pteroServer.identifier}) via Pterodactyl API...")
                    PterodactylService.sendPowerSignal(pteroServer.identifier, "start")
                } else {
                    logger.error("Server ${pteroServer.name} failed to install within 5 minutes. Skipping file synchronization.")
                }
            }
        } else {
            logger.error("Failed to provision Pterodactyl server for group ${group.name}")
        }
    }

    fun stop() {
        running = false
        scope.cancel()
        logger.info("Stratus Orchestrator stopped.")
    }
}
