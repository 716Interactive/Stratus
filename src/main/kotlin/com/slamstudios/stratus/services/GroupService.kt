package com.slamstudios.stratus.services

import com.slamstudios.stratus.db.schema.ServerGroups
import com.slamstudios.stratus.db.schema.ServerState
import com.slamstudios.stratus.db.schema.Servers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

@Serializable
data class ServerGroup(
    val id: String,
    val name: String,
    val templateId: String,
    val minServers: Int,
    val maxServers: Int,
    val targetFreeSlots: Int,
    val scaleDownCooldownSeconds: Int,
    val preferredNodeId: String?,
    val schedulingStrategy: String,
    val metadata: String?
)

object GroupService {
    private val logger = LoggerFactory.getLogger(GroupService::class.java)

    fun getAll(): List<ServerGroup> = transaction {
        ServerGroups.selectAll().map { it.toServerGroup() }
    }

    fun getById(id: String): ServerGroup? = transaction {
        ServerGroups.selectAll().where { ServerGroups.id eq id }.map { it.toServerGroup() }.singleOrNull()
    }
    
    fun getByName(name: String): ServerGroup? = transaction {
        ServerGroups.selectAll().where { ServerGroups.name eq name }.map { it.toServerGroup() }.singleOrNull()
    }

    fun allocate(groupName: String, metaFilter: String? = null): ManagedServer? {
        val group = getByName(groupName) ?: return null
        
        if (metaFilter == null) {
            val redisKey = "stratus:group:${group.id}:ready"
            val serverId = RedisService.resource { it.spop(redisKey) }
            
            if (serverId != null) {
                updateServerToInGame(serverId)
                return ServerService.getById(serverId)
            }
        }

        return transaction {
            var query = Servers.selectAll()
                .where { (Servers.groupId eq group.id) and (Servers.state eq ServerState.READY.name) }
            
            if (metaFilter != null) {
                query = query.andWhere { Servers.metadata eq metaFilter }
            }

            val serverRow = query.limit(1).singleOrNull() ?: return@transaction null
            
            val serverId = serverRow[Servers.id]
            
            Servers.update({ Servers.id eq serverId }) {
                it[state] = ServerState.IN_GAME.name
            }
            
            RedisService.resource { it.srem("stratus:group:${group.id}:ready", serverId) }
            
            val updated = serverRow.toManagedServer().copy(state = ServerState.IN_GAME)
            RedisService.publish("stratus:server:state", "{\"serverId\":\"$serverId\", \"state\":\"IN_GAME\"}")
            
            updated
        }
    }

    private fun updateServerToInGame(id: String) = transaction {
        Servers.update({ Servers.id eq id }) {
            it[state] = ServerState.IN_GAME.name
        }
        RedisService.publish("stratus:server:state", "{\"serverId\":\"$id\", \"state\":\"IN_GAME\"}")
    }

    fun create(
        name: String,
        templateId: String,
        min: Int,
        max: Int,
        target: Int,
        cooldown: Int,
        metadata: String?
    ): ServerGroup = transaction {
        val id = UUID.randomUUID().toString()
        ServerGroups.insert {
            it[ServerGroups.id] = id
            it[ServerGroups.name] = name
            it[ServerGroups.templateId] = templateId
            it[ServerGroups.minServers] = min
            it[ServerGroups.maxServers] = max
            it[ServerGroups.targetFreeSlots] = target
            it[ServerGroups.scaleDownCooldownSeconds] = cooldown
            it[ServerGroups.preferredNodeId] = null
            it[ServerGroups.schedulingStrategy] = "SPREAD"
            it[ServerGroups.metadata] = metadata
        }
        ServerGroup(id, name, templateId, min, max, target, cooldown, null, "SPREAD", metadata)
    }

    fun update(
        id: String,
        templateId: String?,
        min: Int?,
        max: Int?,
        target: Int?,
        cooldown: Int?,
        metadata: String?
    ) = transaction {
        ServerGroups.update({ ServerGroups.id eq id }) {
            if (templateId != null) it[ServerGroups.templateId] = templateId
            if (min != null) it[ServerGroups.minServers] = min
            if (max != null) it[ServerGroups.maxServers] = max
            if (target != null) it[ServerGroups.targetFreeSlots] = target
            if (cooldown != null) it[ServerGroups.scaleDownCooldownSeconds] = cooldown
            if (metadata != null) it[ServerGroups.metadata] = metadata
        }
    }

    fun delete(id: String) = transaction {
        ServerGroups.deleteWhere { ServerGroups.id eq id }
    }

    private fun ResultRow.toServerGroup() = ServerGroup(
        id = this[ServerGroups.id],
        name = this[ServerGroups.name],
        templateId = this[ServerGroups.templateId],
        minServers = this[ServerGroups.minServers],
        maxServers = this[ServerGroups.maxServers],
        targetFreeSlots = this[ServerGroups.targetFreeSlots],
        scaleDownCooldownSeconds = this[ServerGroups.scaleDownCooldownSeconds],
        preferredNodeId = this[ServerGroups.preferredNodeId],
        schedulingStrategy = this[ServerGroups.schedulingStrategy],
        metadata = this[ServerGroups.metadata]
    )

    private fun ResultRow.toManagedServer() = ManagedServer(
        id = this[Servers.id],
        pterodactylId = this[Servers.pterodactylId],
        nodeId = this[Servers.nodeId],
        groupId = this[Servers.groupId],
        templateVersionId = this[Servers.templateVersionId],
        host = this[Servers.host],
        port = this[Servers.port],
        memory = this[Servers.memory],
        disk = this[Servers.disk],
        state = ServerState.fromString(this[Servers.state]),
        players = this[Servers.players],
        autoProxyAdd = this[Servers.autoProxyAdd],
        metadata = this[Servers.metadata],
        lastHeartbeat = this[Servers.lastHeartbeat]?.toString(),
        stateChangedAt = this[Servers.stateChangedAt].toString(),
        createdAt = this[Servers.createdAt].toString()
    )
}
