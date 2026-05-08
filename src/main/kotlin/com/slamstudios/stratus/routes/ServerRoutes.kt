package com.slamstudios.stratus.routes

import com.slamstudios.stratus.db.schema.ServerState
import com.slamstudios.stratus.services.ServerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HeartbeatRequest(val players: Int, val metadata: String? = null)

@Serializable
data class StateUpdateRequest(val state: String)

fun Route.serverRoutes() {
    route("/servers") {
        get {
            val groupId = call.parameters["groupId"]
            val stateStr = call.parameters["state"]
            val state = stateStr?.let { ServerState.fromString(it) }
            
            call.respond(ServerService.getAll(groupId, state))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val server = ServerService.getById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(server)
        }

        post("/{id}/heartbeat") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<HeartbeatRequest>()
            
            ServerService.heartbeat(id, req.players, req.metadata)
            call.respond(mapOf("ok" to true))
        }

        post("/{id}/state") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<StateUpdateRequest>()
            val newState = try {
                ServerState.fromString(req.state)
            } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "INVALID_STATE"))
            }

            // Only allow READY, IN_GAME, ENDING via this endpoint as per plan
            val allowedExternal = setOf(ServerState.READY, ServerState.IN_GAME, ServerState.ENDING)
            if (newState !in allowedExternal) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "TRANSITION_NOT_ALLOWED_EXTERNALLY"))
            }

            ServerService.updateState(id, newState)
            call.respond(mapOf("ok" to true, "state" to newState.name))
        }
    }
}
