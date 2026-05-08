package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/** Maps to the `template_versions` table. */
object TemplateVersions : Table("template_versions") {
    val id            = char("id", 36)
    val templateId    = char("template_id", 36).references(Templates.id)
    val versionNumber = integer("version_number")
    val eggId         = integer("egg_id")
    /** Raw JSON stored as text; parsed by the service layer. */
    val configJson    = text("config_json")
    val createdAt     = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
