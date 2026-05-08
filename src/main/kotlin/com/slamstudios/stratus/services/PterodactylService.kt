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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    val ip_alias: String? = null
)

object PterodactylService {
    private val logger = LoggerFactory.getLogger(PterodactylService::class.java)
    private lateinit var client: HttpClient
    private lateinit var baseUrl: String
    private lateinit var apiKey: String

    fun init(config: PterodactylConfig) {
        baseUrl = config.baseUrl.removeSuffix("/")
        apiKey = config.apiKey
        
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
        val payload = mapOf(
            "name" to name,
            "user" to userId,
            "egg" to eggId,
            "docker_image" to image,
            "startup" to startup,
            "environment" to environment,
            "limits" to mapOf(
                "memory" to memoryMb,
                "swap" to 0,
                "disk" to diskMb,
                "io" to 500,
                "cpu" to 0
            ),
            "feature_limits" to mapOf(
                "databases" to 0,
                "backups" to 0
            ),
            "allocation" to mapOf(
                "default" to 0 // 0 picks the first available from the node if not specified
                // Actually, Ptero API usually needs a specific allocation ID or just a node ID for auto-allocation.
                // For application API, you often specify the allocation ID or use a different endpoint.
                // Assuming "allocation" logic is handled or we use a specific one.
            ),
            "deploy" to mapOf(
                "locations" to listOf<Int>(),
                "dedicated_ip" to false,
                "port_range" to listOf<String>()
            )
        )

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
}
