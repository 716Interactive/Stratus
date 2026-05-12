package com.slamstudios.stratus.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

object FileService {
    private val logger = LoggerFactory.getLogger(FileService::class.java)
    private val client = HttpClient()

    suspend fun listFiles(nodeId: String, path: String): String? {
        val node = NodeService.getById(nodeId) ?: return null
        val wingsUrl = "https://${node.host}:8080" // Assuming standard Wings port
        
        return try {
            val response = client.get("$wingsUrl/api/servers/templates/files/list") {
                header("Authorization", "Bearer ${node.token}") // We need a way to get node token
                parameter("directory", path)
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("Failed to list files on node $nodeId: ${e.message}")
            null
        }
    }
    
    // Additional methods for read, write, delete would go here
}
