package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.TemplateService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateTemplateRequest(val name: String)

@Serializable
data class CreateVersionRequest(val eggId: Int, val config: String)

@Serializable
data class TemplateResponse(
    val template: com.slamstudios.stratus.services.Template,
    val versions: List<com.slamstudios.stratus.services.TemplateVersion>
)

fun Route.templateRoutes() {
    route("/templates") {
        get {
            call.respond(TemplateService.getAll())
        }

        post {
            val req = call.receive<CreateTemplateRequest>()
            call.respond(TemplateService.createTemplate(req.name))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val template = TemplateService.getById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            val versions = TemplateService.getVersions(id)
            call.respond(TemplateResponse(template, versions))
        }

        post("/{id}/versions") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<CreateVersionRequest>()
            call.respond(TemplateService.createVersion(id, req.eggId, req.config))
        }
    }
}
