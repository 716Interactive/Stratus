package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table

/** Maps to the `server_groups` table. */
object ServerGroups : Table("server_groups") {
    val id                       = char("id", 36)
    val name                     = varchar("name", 100)
    val templateId               = char("template_id", 36).references(Templates.id)
    val minServers               = integer("min_servers")
    val maxServers               = integer("max_servers")
    val targetFreeSlots          = integer("target_free_slots")
    val scaleDownCooldownSeconds = integer("scale_down_cooldown_seconds")
    val autoProxyAdd             = bool("auto_proxy_add").default(true)
    val preferredNodeId          = char("preferred_node_id", 36).references(Nodes.id).nullable()
    val schedulingStrategy       = varchar("scheduling_strategy", 20).default("SPREAD")
    /** Optional JSON metadata (game type, map, etc.) stored as text. */
    val metadata                 = text("metadata").nullable()

    override val primaryKey = PrimaryKey(id)
}
