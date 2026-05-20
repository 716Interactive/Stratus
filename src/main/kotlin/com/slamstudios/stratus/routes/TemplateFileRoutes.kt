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

private fun getLatestVersionPath(templateId: String): String {
    val latestVersion = TemplateService.getVersions(templateId).firstOrNull()
        ?: throw IllegalArgumentException("No version found for template")
    val path = java.io.File("/var/lib/pterodactyl/templates/$templateId/${latestVersion.id}")
    if (!path.exists()) {
        path.mkdirs()
    }
    return path.absolutePath
}

private fun checkoutNewVersionForFileModification(templateId: String): String {
    val latestVersion = TemplateService.getVersions(templateId).firstOrNull()
        ?: throw IllegalArgumentException("No version found for template")
    
    // Create a new version with the same egg and config as the latest version
    val newVersion = TemplateService.createVersion(templateId, latestVersion.eggId, latestVersion.configJson)
    return "/var/lib/pterodactyl/templates/$templateId/${newVersion.id}"
}

fun Route.templateFileRoutes() {
    route("/templates/{id}/files") {
        get("/list") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val path = call.request.queryParameters["directory"] ?: "/"
            
            val basePath = getLatestVersionPath(id)
            val files = FileService.listFiles(basePath, path)
            call.respond(files)
        }

        get("/contents") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val file = call.request.queryParameters["file"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val basePath = getLatestVersionPath(id)
            call.respondText(FileService.getFileContents(basePath, file))
        }

        post("/write") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val file = call.request.queryParameters["file"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val content = call.receiveText()
            val basePath = checkoutNewVersionForFileModification(id)
            FileService.writeFileContents(basePath, file, content)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        post("/delete") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val files = call.receive<List<String>>()
            val basePath = checkoutNewVersionForFileModification(id)
            FileService.deleteFiles(basePath, files)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        @Serializable
        data class RenamePayload(val from: String, val to: String)

        post("/rename") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val payload = call.receive<RenamePayload>()
            val basePath = checkoutNewVersionForFileModification(id)
            FileService.renameFile(basePath, payload.from, payload.to)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        @Serializable
        data class CompressPayload(val directory: String, val files: List<String>)

        post("/compress") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val payload = call.receive<CompressPayload>()
            val basePath = checkoutNewVersionForFileModification(id)
            val archiveName = FileService.compressFiles(basePath, payload.directory, payload.files)
            call.respond(mapOf("archive" to archiveName))
        }

        @Serializable
        data class DecompressPayload(val file: String)

        post("/decompress") {
            val id = call.parameters["id"] ?: return@post call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val payload = call.receive<DecompressPayload>()
            val basePath = checkoutNewVersionForFileModification(id)
            FileService.decompressFile(basePath, payload.file)
            call.respond(io.ktor.http.HttpStatusCode.NoContent)
        }

        get("/download") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val fileParam = call.request.queryParameters["file"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val basePath = getLatestVersionPath(id)
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
            val basePath = checkoutNewVersionForFileModification(id)

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is io.ktor.http.content.PartData.FileItem) {
                    val fileName = part.originalFileName ?: "file"
                    part.streamProvider().use { input ->
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
