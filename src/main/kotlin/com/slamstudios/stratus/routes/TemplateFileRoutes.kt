package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.FileService
import com.slamstudios.stratus.services.TemplateService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.jvm.javaio.toInputStream

fun Route.templateFileRoutes() {
    route("/templates/{id}/files") {
        get("/list") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val path = call.request.queryParameters["directory"] ?: "/"
            
            val template = TemplateService.getById(id) ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            
            val files = FileService.listFiles(basePath, path)
            call.respond(files)
        }

        get("/contents") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val file = call.request.queryParameters["file"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val template = TemplateService.getById(id) ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            call.respondText(FileService.getFileContents(basePath, file))
        }

        post("/write") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val file = call.request.queryParameters["file"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val content = call.receiveText()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            FileService.writeFileContents(basePath, file, content)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        post("/delete") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val files = call.receive<List<String>>()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            FileService.deleteFiles(basePath, files)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        post("/upload") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val directory = call.request.queryParameters["directory"] ?: "/"
            val extract = call.request.queryParameters["extract"]?.toBoolean() ?: false
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is io.ktor.http.content.PartData.FileItem) {
                    val fileName = part.originalFileName ?: "file"
                    part.provider().toInputStream().use { input ->
                        if (extract && fileName.endsWith(".zip", ignoreCase = true)) {
                            FileService.extractZip(basePath, directory, input)
                         } else {
                            FileService.saveFile(basePath, "$directory/$fileName", input)
                         }
                    }
                }
                part.dispose()
            }
            call.respond(io.ktor.http.HttpStatusCode.OK)
        }
    }
}
