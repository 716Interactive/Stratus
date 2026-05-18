package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.FileService
import com.slamstudios.stratus.services.TemplateService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.Serializable

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

        @Serializable
        data class RenamePayload(val from: String, val to: String)

        post("/rename") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val payload = call.receive<RenamePayload>()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            FileService.renameFile(basePath, payload.from, payload.to)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        @Serializable
        data class CompressPayload(val directory: String, val files: List<String>)

        post("/compress") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val payload = call.receive<CompressPayload>()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            val archiveName = FileService.compressFiles(basePath, payload.directory, payload.files)
            call.respond(mapOf("archive" to archiveName))
        }

        @Serializable
        data class DecompressPayload(val file: String)

        post("/decompress") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val payload = call.receive<DecompressPayload>()
            val template = TemplateService.getById(id) ?: return@post call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            FileService.decompressFile(basePath, payload.file)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        get("/download") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val fileParam = call.request.queryParameters["file"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val template = TemplateService.getById(id) ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound)
            val basePath = java.io.File(template.localPath, template.id).absolutePath
            val targetFile = java.io.File(basePath, fileParam.trimStart('/')).canonicalFile
            if (!targetFile.absolutePath.startsWith(java.io.File(basePath).canonicalFile.absolutePath)) {
                return@get call.respond(io.ktor.http.HttpStatusCode.Forbidden)
            }
            if (targetFile.exists() && targetFile.isFile) {
                call.response.header(io.ktor.http.HttpHeaders.ContentDisposition, io.ktor.http.ContentDisposition.Attachment.withParameter(io.ktor.http.ContentDisposition.Parameters.FileName, targetFile.name).toString())
                call.respondFile(targetFile)
            } else {
                call.respond(io.ktor.http.HttpStatusCode.NotFound)
            }
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
