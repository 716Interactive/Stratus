package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table

/** Maps to the `nodes` table. */
object Nodes : Table("nodes") {
    val id            = char("id", 36)
    val pterodactylId = integer("pterodactyl_id")
    val name          = varchar("name", 100)
    val host          = varchar("host", 255).default("localhost")
    val token         = text("token").default("")
    val totalMemory   = integer("total_memory")
    val totalDisk     = integer("total_disk")
    val allocatedMemory = integer("allocated_memory").default(0)
    val allocatedDisk   = integer("allocated_disk").default(0)

    override val primaryKey = PrimaryKey(id)
}
