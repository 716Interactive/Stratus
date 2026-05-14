package com.slamstudios.stratus.routes

import com.slamstudios.stratus.services.AuditLogs
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class AuditLogEntry(
    val id: Int,
    val level: String,
    val category: String,
    val message: String,
    val metadata: String?,
    val createdAt: String
)

fun Route.auditRoutes() {
    route("/audit") {
        get {
            val logs = transaction {
                AuditLogs.selectAll()
                    .orderBy(AuditLogs.createdAt, SortOrder.DESC)
                    .limit(50)
                    .map {
                        AuditLogEntry(
                            id = it[AuditLogs.id],
                            level = it[AuditLogs.level],
                            category = it[AuditLogs.category],
                            message = it[AuditLogs.message],
                            metadata = it[AuditLogs.metadata],
                            createdAt = it.getOrNull<LocalDateTime>(AuditLogs.createdAt).toString()
                        )
                    }
            }
            call.respond(logs)
        }
    }
}
