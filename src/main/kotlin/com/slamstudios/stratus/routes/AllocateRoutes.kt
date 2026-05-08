package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.GroupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class AllocateRequest(
    val group: String,
    val meta: String? = null
)

@Serializable
data class AllocateResponse(
    val serverId: String,
    val host: String,
    val port: Int,
    val group: String,
    val templateVersion: String
)

fun Route.allocateRoutes() {
    post("/allocate") {
        val req = call.receive<AllocateRequest>()
        val server = GroupService.allocate(req.group, req.meta)
        
        if (server == null) {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                "error" to "NO_SERVERS_AVAILABLE",
                "group" to req.group
            ))
        } else {
            call.respond(AllocateResponse(
                serverId = server.id,
                host = server.host,
                port = server.port,
                group = req.group,
                templateVersion = server.templateVersionId
            ))
        }
    }
}
