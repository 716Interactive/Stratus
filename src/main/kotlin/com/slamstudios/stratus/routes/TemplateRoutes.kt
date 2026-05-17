package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.TemplateService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreateTemplateRequest(val name: String, val ownerId: Int = 1)

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
            call.respond(TemplateService.createTemplate(req.name, req.ownerId))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val template = TemplateService.getById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            val versions = TemplateService.getVersions(id)
            call.respond(TemplateResponse(template, versions))
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<UpdateTemplateRequest>()
            
            if (req.name != null) {
                TemplateService.updateTemplateName(id, req.name)
            }
            
            val latestVersion = TemplateService.getVersions(id).firstOrNull()
            val currentEggId = req.eggId ?: latestVersion?.eggId ?: 1
            
            val currentConfig = latestVersion?.configJson?.let {
                try {
                    Json.decodeFromString<com.slamstudios.stratus.services.PteroTemplateConfig>(it)
                } catch (e: Exception) {
                    com.slamstudios.stratus.services.PteroTemplateConfig()
                }
            } ?: com.slamstudios.stratus.services.PteroTemplateConfig()
            
            val newConfig = currentConfig.copy(
                memory = req.memory ?: currentConfig.memory,
                disk = req.disk ?: currentConfig.disk,
                cpu = req.cpu ?: currentConfig.cpu,
                startup = req.startup ?: currentConfig.startup,
                image = req.image ?: currentConfig.image,
                env = req.environment ?: currentConfig.env
            )
            
            val newConfigJson = Json.encodeToString(com.slamstudios.stratus.services.PteroTemplateConfig.serializer(), newConfig)
            TemplateService.createVersion(id, currentEggId, newConfigJson)
            
            call.respond(mapOf("ok" to true))
        }

        post("/{id}/versions") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val req = call.receive<CreateVersionRequest>()
            call.respond(TemplateService.createVersion(id, req.eggId, req.config))
        }
    }
}

@Serializable
data class UpdateTemplateRequest(
    val name: String? = null,
    val eggId: Int? = null,
    val memory: Int? = null,
    val disk: Int? = null,
    val cpu: Int? = null,
    val startup: String? = null,
    val image: String? = null,
    val environment: Map<String, String>? = null
)
