package com.slamstudios.stratus.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * The core Stratus API entry point for Minecraft plugins.
 * Use StratusAPI.get() to access the singleton network client.
 */
object StratusAPI {
    private var client: StratusClient? = null

    /**
     * Retrieves the active Stratus client instance.
     * 
     * If the client has not been manually initialized via [init], it will attempt
     * to automatically discover credentials from the environment variables 
     * (STRATUS_URL, STRATUS_TOKEN, STRATUS_SERVER_ID).
     *
     * @return The initialized [StratusClient].
     * @throws IllegalStateException if environment variables are missing and init() was not called.
     */
    fun get(): StratusClient {
        if (client == null) {
            client = StratusClient.fromEnv() ?: throw IllegalStateException("Stratus environment not detected (STRATUS_URL/TOKEN/SERVER_ID missing)")
        }
        return client!!
    }

    /**
     * Manually initializes the API with specific credentials.
     * Use this if you are running in an environment where standard environment
     * variables are not available or need to be overridden.
     *
     * @param url The public URL of the Stratus Orchestrator.
     * @param token The secret API token for authentication.
     * @param serverId The unique ID assigned to this server instance.
     */
    fun init(url: String, token: String, serverId: String) {
        client = StratusClient(url, token, serverId)
    }
}

/**
 * A high-level client for interacting with the Stratus Orchestrator.
 * Handles HTTP communication, serialization, and authentication.
 */
class StratusClient(
    private val orchestratorUrl: String,
    private val token: String,
    private val serverId: String
) {
    private val logger = LoggerFactory.getLogger(StratusClient::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Reports a heartbeat to the orchestrator.
     * This should be called periodically (e.g., every 30 seconds) to ensure
     * the server is not flagged as "stuck" by the Watchdog.
     *
     * @param players The current number of players online.
     * @param metadata Optional JSON or string metadata describing the server's current state (e.g., map name).
     * @return true if the heartbeat was successfully delivered.
     */
    suspend fun heartbeat(players: Int, metadata: String? = null): Boolean {
        return try {
            val response = client.post("$orchestratorUrl/servers/$serverId/heartbeat") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("players" to players, "metadata" to metadata))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error("Failed to send heartbeat to Stratus: ${e.message}")
            false
        }
    }

    /**
     * Updates the server's lifecycle state in the orchestrator.
     * Standard states include: STARTING, READY, IN_GAME, EMPTY, DRAINING, TERMINATED.
     *
     * @param state The new state to transition to.
     * @return true if the state update was accepted.
     */
    suspend fun updateState(state: String): Boolean {
        return try {
            val response = client.post("$orchestratorUrl/servers/$serverId/state") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("state" to state))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error("Failed to update state to $state in Stratus: ${e.message}")
            false
        }
    }

    suspend fun getSelfDetails(): String? {
        return try {
            val response = client.get("$orchestratorUrl/servers/$serverId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status.isSuccess()) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Dynamically fetches the primary proxy's security token.
     */
    suspend fun getProxyToken(): String? {
        return try {
            val response = client.get("$orchestratorUrl/servers/$serverId/proxy-token") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status.isSuccess()) response.bodyAsText().trim() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Dynamically registers a proxy instance with the Orchestrator.
     */
    suspend fun registerProxy(name: String, host: String, port: Int, isMain: Boolean, token: String): Boolean {
        return try {
            val response = client.post("$orchestratorUrl/proxies") {
                header(HttpHeaders.Authorization, "Bearer ${this@StratusClient.token}")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "name" to name,
                    "host" to host,
                    "port" to port,
                    "isMain" to isMain,
                    "token" to token
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error("Failed to register proxy: ${e.message}")
            false
        }
    }

    /**
     * Closes the underlying HTTP client.
     */
    fun close() {
        client.close()
    }

    companion object {
        /**
         * Attempts to create a client by reading STRATUS_URL, STRATUS_TOKEN, and STRATUS_SERVER_ID
         * from the process environment variables.
         *
         * @return A [StratusClient] if all variables are present, otherwise null.
         */
        fun fromEnv(): StratusClient? {
            val url = System.getenv("STRATUS_URL") ?: return null
            val token = System.getenv("STRATUS_TOKEN") ?: return null
            val id = System.getenv("STRATUS_SERVER_ID") ?: return null
            return StratusClient(url, token, id)
        }
    }
}
