package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.GroupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    val name: String,
    val templateId: String,
    val minServers: Int,
    val maxServers: Int,
    val targetFreeSlots: Int,
    val scaleDownCooldownSeconds: Int,
    val metadata: String? = null
)

@Serializable
data class UpdateGroupRequest(
    val templateId: String? = null,
    val minServers: Int? = null,
    val maxServers: Int? = null,
    val targetFreeSlots: Int? = null,
    val scaleDownCooldownSeconds: Int? = null,
    val metadata: String? = null
)

fun Route.groupRoutes() {
    route("/groups") {
        get {
            call.respond(GroupService.getAll())
        }

        post {
            val req = call.receive<CreateGroupRequest>()
            val group = GroupService.create(
                req.name, req.templateId, req.minServers, req.maxServers,
                req.targetFreeSlots, req.scaleDownCooldownSeconds, req.metadata
            )
            call.respond(group)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val group = GroupService.getById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(group)
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<UpdateGroupRequest>()
            GroupService.update(
                id, req.templateId, req.minServers, req.maxServers,
                req.targetFreeSlots, req.scaleDownCooldownSeconds, req.metadata
            )
            call.respond(mapOf("ok" to true))
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            GroupService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
