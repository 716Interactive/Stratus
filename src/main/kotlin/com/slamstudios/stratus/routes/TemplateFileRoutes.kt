package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.FileService
import com.slamstudios.stratus.services.TemplateService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.templateFileRoutes() {
    route("/templates/{id}/files") {
        get("/list") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val path = call.request.queryParameters["directory"] ?: "/"
            
            val template = TemplateService.getById(id) ?: return@get call.respond(io.ktor.http.HttpStatusCode.NotFound)
            
            // For templates, we use the first available node or a master node to manage files
            // For now, we'll use a hardcoded node ID or fetch it from config
            val nodeId = "MASTER_NODE_ID" 
            
            val files = FileService.listFiles(nodeId, path)
            if (files != null) {
                call.respondText(files, io.ktor.http.ContentType.Application.Json)
            } else {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError)
            }
        }
    }
}
