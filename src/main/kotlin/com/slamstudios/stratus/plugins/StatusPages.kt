package com.slamstudios.stratus.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StatusPages")

@Serializable
data class ErrorResponse(val error: String, val message: String? = null)

fun Application.configureStatusPages() {
    install(StatusPages) {

        // ── 400 Bad Request ───────────────────────────────────────────────────
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Bad request: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("BAD_REQUEST", cause.message)
            )
        }

        // ── 404 Not Found ────────────────────────────────────────────────────
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("NOT_FOUND", "The requested resource does not exist.")
            )
        }

        // ── 409 Conflict ─────────────────────────────────────────────────────
        exception<IllegalStateException> { call, cause ->
            logger.warn("Conflict: ${cause.message}")
            call.respond(
                HttpStatusCode.Conflict,
                ErrorResponse("CONFLICT", cause.message)
            )
        }

        // ── 500 Internal Server Error ─────────────────────────────────────────
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred.")
            )
        }
    }
}
