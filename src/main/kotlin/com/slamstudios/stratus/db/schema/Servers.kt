package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Valid lifecycle states for a managed server.
 * Transitions are enforced by the orchestrator service layer.
 *
 *   STARTING → READY → IN_GAME → ENDING → EMPTY → DRAINING → TERMINATED
 */
enum class ServerState {
    STARTING,
    READY,
    IN_GAME,
    ENDING,
    EMPTY,
    DRAINING,
    TERMINATED;

    companion object {
        /** Terminal states — servers in these states will never transition again. */
        val TERMINAL = setOf(TERMINATED)

        /** States that are visible to external allocators. */
        val ALLOCATABLE = setOf(READY)

        /** States considered "active" for capacity counting. */
        val ACTIVE = setOf(STARTING, READY, IN_GAME, ENDING, EMPTY, DRAINING)

        fun fromString(value: String): ServerState =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown ServerState: $value")
    }
}

/** Maps to the `servers` table. */
object Servers : Table("servers") {
    val id                = char("id", 36)
    val pterodactylId     = integer("pterodactyl_id").nullable()
    val nodeId            = char("node_id", 36).references(Nodes.id)
    val groupId           = char("group_id", 36).references(ServerGroups.id)
    val templateVersionId = char("template_version_id", 36).references(TemplateVersions.id)
    val host              = varchar("host", 45)
    val port              = integer("port")
    val memory            = integer("memory").default(0)
    val disk              = integer("disk").default(0)
    /** Stored as VARCHAR matching the ENUM values defined in SQL. */
    val state             = varchar("state", 20)
    val players           = integer("players")
    val autoProxyAdd      = bool("auto_proxy_add").default(true)
    val metadata          = text("metadata").nullable()
    val lastHeartbeat     = datetime("last_heartbeat").nullable()
    val stateChangedAt    = datetime("state_changed_at")
    val createdAt         = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
