package com.slamstudios.stratus.plugins

import com.slamstudios.stratus.config.AppConfig
import com.slamstudios.stratus.routes.allocateRoutes
import com.slamstudios.stratus.routes.groupRoutes
import com.slamstudios.stratus.routes.healthRoutes
import com.slamstudios.stratus.routes.serverRoutes
import com.slamstudios.stratus.routes.templateRoutes
import com.slamstudios.stratus.routes.proxyRoutes
import com.slamstudios.stratus.routes.auditRoutes
import com.slamstudios.stratus.routes.nodeRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {

    // ── Token Authentication ───────────────────────────────────────────────
    install(Authentication) {
        bearer("stratus-token") {
            authenticate { credential ->
                if (credential.token == config.token) {
                    UserIdPrincipal("stratus")
                } else {
                    null
                }
            }
        }
    }

    routing {
        // ── Public ────────────────────────────────────────────────────────
        healthRoutes()

        // ── Protected (token required) ────────────────────────────────────
        authenticate("stratus-token") {
            serverRoutes()
            allocateRoutes()
            groupRoutes()
            templateRoutes()
            proxyRoutes()
            auditRoutes()
            nodeRoutes()
        }
    }
}
