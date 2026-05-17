package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table

/** Maps to the `templates` table. */
object Templates : Table("templates") {
    val id               = char("id", 36)
    val name             = varchar("name", 100)
    /** Nullable FK → template_versions.id — managed in SQL to avoid circular reference. */
    val currentVersionId = char("current_version_id", 36).nullable()
    val localPath        = varchar("local_path", 255).default("/var/lib/pterodactyl/templates")
    val ownerId          = integer("owner_id").default(1)

    override val primaryKey = PrimaryKey(id)
}
