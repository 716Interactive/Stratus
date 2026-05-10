package com.slamstudios.stratus.db.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ProxyGroups : Table("proxy_groups") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val metadata = text("metadata").nullable()
    override val primaryKey = PrimaryKey(id)
}

object Proxies : Table("proxies") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val host = varchar("host", 255)
    val port = integer("port")
    val proxyGroupId = varchar("proxy_group_id", 36).references(ProxyGroups.id).nullable()
    val isMain = bool("is_main").default(false)
    val isStatic = bool("is_static").default(true)
    val lastHeartbeat = datetime("last_heartbeat").nullable()
    override val primaryKey = PrimaryKey(id)
}
