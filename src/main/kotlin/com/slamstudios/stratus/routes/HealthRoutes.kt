package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.RedisService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class HealthResponse(
    val status: String,
    val db: String,
    val redis: String,
    val version: String,
)

fun Routing.healthRoutes() {
    get("/health") {
        val dbOk = try {
            transaction {
                exec("SELECT 1") { rs -> rs.next() }
                "ok"
            }
        } catch (e: Exception) {
            "error: ${e.message}"
        }

        val redisOk = try {
            RedisService.resource { it.ping() }
            "ok"
        } catch (e: Exception) {
            "error: ${e.message}"
        }

        val status = if (dbOk == "ok" && redisOk == "ok") "ok" else "degraded"

        call.respond(
            HttpStatusCode.OK,
            HealthResponse(
                status  = status,
                db      = dbOk,
                redis   = redisOk,
                version = "1.0.0",
            )
        )
    }
}
