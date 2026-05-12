package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.NodeService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.nodeRoutes() {
    route("/nodes") {
        get {
            call.respond(NodeService.getAll())
        }
    }
}
