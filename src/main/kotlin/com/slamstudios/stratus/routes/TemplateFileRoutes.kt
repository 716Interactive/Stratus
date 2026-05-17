package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.FileService
import com.slamstudios.stratus.services.TemplateService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.forEachPart
import kotlinx.io.asInputStream

fun Route.templateFileRoutes() {
    route("/templates/{id}/files") {
        get("/list") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val path = call.request.queryParameters["directory"] ?: "/"
            
            val template = TemplateService.getById(id) ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound)
            
            // Templates are stored locally on the Orchestrator base path
            val files = FileService.listFiles(template.localPath, path)
            call.respond(files)
        }

        get("/contents") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val file = call.request.queryParameters["file"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val template = TemplateService.getById(id) ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound)
            call.respondText(FileService.getFileContents(template.localPath, file))
        }

        post("/write") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val file = call.request.queryParameters["file"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val content = call.receiveText()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            FileService.writeFileContents(template.localPath, file, content)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        post("/delete") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val files = call.receive<List<String>>()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            FileService.deleteFiles(template.localPath, files)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        post("/upload") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val directory = call.request.queryParameters["directory"] ?: "/"
            val extract = call.request.queryParameters["extract"]?.toBoolean() ?: false
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is io.ktor.http.content.PartData.FileItem) {
                    val fileName = part.originalFileName ?: "file"
                    part.provider().asInputStream().use { input ->
                        if (extract && fileName.endsWith(".zip", ignoreCase = true)) {
                            FileService.extractZip(template.localPath, directory, input)
                        } else {
                            FileService.saveFile(template.localPath, "$directory/$fileName", input)
                        }
                    }
                }
                part.dispose()
            }
            call.respond(io.ktor.http.HttpStatusCode.OK)
        }
    }
}
