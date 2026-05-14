package com.slamstudios.stratus.services

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object AuditLogs : Table("audit_logs") {
    val id = integer("id").autoIncrement()
    val level = varchar("level", 20)
    val category = varchar("category", 50)
    val message = text("message")
    val metadata = text("metadata").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(id)
}

object AuditService {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    fun log(level: String, category: String, message: String, metadata: String? = null) {
        transaction {
            AuditLogs.insert {
                it[AuditLogs.level] = level
                it[AuditLogs.category] = category
                it[AuditLogs.message] = message
                it[AuditLogs.metadata] = metadata
            }
        }
        logger.info("[$category] $message")
    }

    fun info(category: String, message: String, metadata: String? = null) = log("INFO", category, message, metadata)
    fun warn(category: String, message: String, metadata: String? = null) = log("WARNING", category, message, metadata)
    fun error(category: String, message: String, metadata: String? = null) = log("ERROR", category, message, metadata)
    fun action(category: String, message: String, metadata: String? = null) = log("ACTION", category, message, metadata)
}
