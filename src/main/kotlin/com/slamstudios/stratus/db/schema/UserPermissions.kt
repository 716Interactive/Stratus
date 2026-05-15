package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table

/** Maps to the `user_permissions` table for Stratus resource sharing. */
object UserPermissions : Table("user_permissions") {
    val id         = integer("id").autoIncrement()
    val userId     = integer("user_id") // Pterodactyl User ID
    val resourceId = char("resource_id", 36) // Group ID or Template ID
    val resourceType = varchar("resource_type", 20) // "GROUP" or "TEMPLATE"
    val permissions = text("permissions") // JSON list of permission strings

    override val primaryKey = PrimaryKey(id)
}
