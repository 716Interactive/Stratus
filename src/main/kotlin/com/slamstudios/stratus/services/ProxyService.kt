package com.slamstudios.stratus.services

import com.slamstudios.stratus.db.schema.Proxies
import com.slamstudios.stratus.db.schema.ProxyGroups
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

data class ProxyInstance(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val proxyGroupId: String?,
    val isMain: Boolean,
    val isStatic: Boolean
)

object ProxyService {
    private val logger = LoggerFactory.getLogger(ProxyService::class.java)

    fun getAll(): List<ProxyInstance> = transaction {
        Proxies.selectAll().map { it.toProxyInstance() }
    }

    fun getMainProxy(): ProxyInstance? = transaction {
        Proxies.selectAll().where { Proxies.isMain eq true }.map { it.toProxyInstance() }.firstOrNull()
    }

    fun registerProxy(name: String, host: String, port: Int, groupId: String? = null, isMain: Boolean = false): ProxyInstance = transaction {
        if (isMain) {
            // Unset other main proxies
            Proxies.update({ Proxies.isMain eq true }) {
                it[Proxies.isMain] = false
            }
        }
        
        val id = UUID.randomUUID().toString()
        Proxies.insert {
            it[Proxies.id] = id
            it[Proxies.name] = name
            it[Proxies.host] = host
            it[Proxies.port] = port
            it[Proxies.proxyGroupId] = groupId
            it[Proxies.isMain] = isMain
        }
        ProxyInstance(id, name, host, port, groupId, isMain, true)
    }

    private fun ResultRow.toProxyInstance() = ProxyInstance(
        id = this[Proxies.id],
        name = this[Proxies.name],
        host = this[Proxies.host],
        port = this[Proxies.port],
        proxyGroupId = this[Proxies.proxyGroupId],
        isMain = this[Proxies.isMain],
        isStatic = this[Proxies.isStatic]
    )
}
