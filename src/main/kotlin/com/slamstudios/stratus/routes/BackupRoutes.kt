package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.BackupService
import com.slamstudios.stratus.services.GoogleDriveConfigs
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class CallbackRequest(val code: String)

@Serializable
data class TokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Int
)

fun Route.backupRoutes() {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    route("/backups") {
        get("/config") {
            call.respond(BackupService.getConfig() ?: HttpStatusCode.NotFound)
        }

        post("/callback") {
            val req = call.receive<CallbackRequest>()
            val config = BackupService.getConfig() ?: return@post call.respond(HttpStatusCode.InternalServerError)

            // Exchange code for tokens
            val response = client.post("https://oauth2.googleapis.com/token") {
                setBody<FormDataContent>(
                    FormDataContent(Parameters.build {
                        append("client_id", config.clientId)
                        append("client_secret", config.clientSecret)
                        append("code", req.code)
                        append("grant_type", "authorization_code")
                        append("redirect_uri", "http://localhost/admin/stratus/backups/callback") // Should match Panel route
                    })
                )
            }

            if (response.status == HttpStatusCode.OK) {
                val tokens: TokenResponse = response.body()
                transaction {
                    GoogleDriveConfigs.update({ GoogleDriveConfigs.id eq 1 }) {
                        it[accessToken] = tokens.access_token
                        if (tokens.refresh_token != null) it[refreshToken] = tokens.refresh_token
                    }
                }
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest, response.bodyAsText())
            }
        }
    }
}
