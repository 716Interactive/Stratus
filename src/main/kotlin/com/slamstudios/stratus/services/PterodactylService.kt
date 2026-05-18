package com.slamstudios.stratus.services

import com.slamstudios.stratus.config.PterodactylConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

@Serializable
data class PteroResponse<T>(val attributes: T)

@Serializable
data class PteroServer(
    val id: Int,
    val uuid: String,
    val identifier: String,
    val name: String,
    val host: String? = null,
    val port: Int? = null
)

@Serializable
data class PteroServerAttributes(
    val id: Int,
    val uuid: String,
    val identifier: String,
    val name: String,
    val relationships: PteroRelationships? = null
)

@Serializable
data class PteroRelationships(
    val allocations: PteroAllocationList? = null
)

@Serializable
data class PteroAllocationList(
    val data: List<PteroResponse<PteroAllocationAttributes>>
)

@Serializable
data class PteroAllocationAttributes(
    val id: Int,
    val ip: String,
    val port: Int,
    val ip_alias: String? = null,
    val assigned: Boolean = false
)

object PterodactylService {
    private val logger = LoggerFactory.getLogger(PterodactylService::class.java)
    private lateinit var client: HttpClient
    private lateinit var baseUrl: String
    private lateinit var apiKey: String
    private var clientApiKey: String? = null

    fun init(config: PterodactylConfig) {
        baseUrl = config.baseUrl.removeSuffix("/")
        apiKey = config.apiKey
        clientApiKey = config.clientApiKey
        
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            defaultRequest {
                url(baseUrl)
                header("Authorization", "Bearer $apiKey")
                header("Accept", "application/json")
                contentType(ContentType.Application.Json)
            }
        }
        logger.info("PterodactylService initialised at $baseUrl")
    }

    suspend fun deleteServer(pteroId: Int) {
        try {
            val response = client.delete("/api/application/servers/$pteroId")
            if (response.status.value !in 200..299) {
                logger.error("Failed to delete Pterodactyl server $pteroId: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error("Exception while deleting Pterodactyl server $pteroId", e)
        }
    }

    suspend fun isServerInstalled(pteroId: Int): Boolean {
        return try {
            val response = client.get("/api/application/servers/$pteroId")
            if (response.status == HttpStatusCode.OK) {
                val text = response.bodyAsText()
                val json = Json.parseToJsonElement(text).jsonObject
                val attributes = json["attributes"]?.jsonObject
                val status = attributes?.get("status")?.jsonPrimitive?.contentOrNull
                status == null
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPowerSignal(serverIdentifier: String, signal: String) {
        try {
            val token = clientApiKey ?: apiKey
            val response = client.post("/api/client/servers/$serverIdentifier/power") {
                headers {
                    set("Authorization", "Bearer $token")
                }
                setBody(mapOf("signal" to signal))
            }
            if (response.status.value !in 200..299) {
                logger.error("Failed to send power signal $signal to server $serverIdentifier: ${response.status} - ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            logger.error("Exception sending power signal $signal to server $serverIdentifier", e)
        }
    }

    suspend fun getUnassignedAllocation(nodeId: Int): Int? {
        return try {
            val response = client.get("/api/application/nodes/$nodeId/allocations")
            if (response.status == HttpStatusCode.OK) {
                val body: PteroAllocationList = response.body()
                body.data.map { it.attributes }.firstOrNull { !it.assigned }?.id
            } else {
                logger.error("Failed to fetch allocations for node $nodeId: ${response.status} - ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            logger.error("Exception while fetching allocations for node $nodeId", e)
            null
        }
    }

    suspend fun createServer(
        name: String,
        userId: Int,
        eggId: Int,
        nodeId: Int,
        memoryMb: Int,
        diskMb: Int,
        startup: String,
        image: String,
        environment: Map<String, String>
    ): PteroServer? {
        val allocationId = getUnassignedAllocation(nodeId)
        if (allocationId == null) {
            logger.error("No free allocations found for node $nodeId. Cannot create server.")
            return null
        }

        // Fetch this egg's variables to guarantee all required variables have a default value
        val eggs = getEggs()
        val egg = eggs.find { it.id == eggId }
        val eggVariables = if (egg != null) {
            getEggVariables(egg.nest, eggId)
        } else {
            emptyMap()
        }

        val payload = buildJsonObject {
            put("name", name)
            put("user", userId)
            put("egg", eggId)
            put("docker_image", image)
            put("startup", startup)
            put("environment", buildJsonObject {
                // 1. Pre-fill egg's default variables
                eggVariables.forEach { (k, v) -> put(k, v) }
                // 2. Override with custom variables from orchestrator/template
                environment.forEach { (k, v) -> put(k, v) }
            })
            put("limits", buildJsonObject {
                put("memory", memoryMb)
                put("swap", 0)
                put("disk", diskMb)
                put("io", 500)
                put("cpu", 0)
            })
            put("feature_limits", buildJsonObject {
                put("databases", 0)
                put("backups", 0)
            })
            put("allocation", buildJsonObject {
                put("default", allocationId)
            })
        }

        return try {
            val response = client.post("/api/application/servers?include=allocations") {
                setBody(payload)
            }
            if (response.status == HttpStatusCode.Created) {
                val body: PteroResponse<PteroServerAttributes> = response.body()
                val attr = body.attributes
                
                // Extract default allocation
                val primaryAllocation = attr.relationships?.allocations?.data
                    ?.map { it.attributes }
                    ?.firstOrNull() // Usually the first one is the default
                
                logger.info("Created Pterodactyl server: ${attr.name} (ID: ${attr.id}) at ${primaryAllocation?.ip}:${primaryAllocation?.port}")
                
                PteroServer(
                    id = attr.id,
                    uuid = attr.uuid,
                    identifier = attr.identifier,
                    name = attr.name,
                    host = primaryAllocation?.ip,
                    port = primaryAllocation?.port
                )
            } else {
                logger.error("Failed to create Pterodactyl server: ${response.status} - ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            logger.error("Exception while creating Pterodactyl server", e)
            null
        }
    }

    suspend fun getNodes(): List<PteroNodeAttributes> {
        return try {
            val response = client.get("/api/application/nodes")
            if (response.status == HttpStatusCode.OK) {
                val body: PteroNodeList = response.body()
                body.data.map { it.attributes }
            } else {
                logger.error("Failed to fetch Pterodactyl nodes: ${response.status} - ${response.bodyAsText()}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Exception while fetching Pterodactyl nodes", e)
            emptyList()
        }
    }

    suspend fun getEggs(): List<PteroEggAttributes> {
        return try {
            val response = client.get("/api/application/nests")
            if (response.status == HttpStatusCode.OK) {
                val nestsBody: PteroResponseList<PteroNestAttributes> = response.body()
                val eggsList = mutableListOf<PteroEggAttributes>()
                for (nest in nestsBody.data) {
                    val nestId = nest.attributes.id
                    val eggsResponse = client.get("/api/application/nests/$nestId/eggs")
                    if (eggsResponse.status == HttpStatusCode.OK) {
                        val eggsBody: PteroResponseList<PteroEggAttributes> = eggsResponse.body()
                        eggsList.addAll(eggsBody.data.map { it.attributes })
                    }
                }
                eggsList
            } else {
                logger.error("Failed to fetch Pterodactyl nests: ${response.status} - ${response.bodyAsText()}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Exception while fetching Pterodactyl eggs", e)
            emptyList()
        }
    }

    suspend fun getEggVariables(nestId: Int, eggId: Int): Map<String, String> {
        return try {
            val response = client.get("/api/application/nests/$nestId/eggs/$eggId?include=variables")
            if (response.status == HttpStatusCode.OK) {
                val text = response.bodyAsText()
                val json = Json.parseToJsonElement(text).jsonObject
                val attributes = json["attributes"]?.jsonObject
                val variables = attributes?.get("relationships")?.jsonObject
                    ?.get("variables")?.jsonObject?.get("data")?.jsonArray
                
                val result = mutableMapOf<String, String>()
                if (variables != null) {
                    for (v in variables) {
                        val vAttr = v.jsonObject["attributes"]?.jsonObject
                        val envVar = vAttr?.get("env_variable")?.jsonPrimitive?.content
                        val defaultVal = vAttr?.get("default_value")?.jsonPrimitive?.content ?: ""
                        if (envVar != null) {
                            result[envVar] = defaultVal
                        }
                    }
                }
                result
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch egg variables for egg $eggId in nest $nestId", e)
            emptyMap()
        }
    }
}

@Serializable
data class PteroResponseList<T>(
    val data: List<PteroResponse<T>>
)

@Serializable
data class PteroNestAttributes(
    val id: Int,
    val name: String
)

@Serializable
data class PteroEggAttributes(
    val id: Int,
    val name: String,
    val nest: Int,
    val docker_images: Map<String, String>? = null
)

@Serializable
data class PteroNodeList(
    val data: List<PteroResponse<PteroNodeAttributes>>
)

@Serializable
data class PteroNodeAttributes(
    val id: Int,
    val name: String,
    val fqdn: String,
    val memory: Int,
    val disk: Int
)
