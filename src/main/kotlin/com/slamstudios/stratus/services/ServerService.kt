package com.slamstudios.stratus.services

import com.slamstudios.stratus.db.schema.Servers
import com.slamstudios.stratus.db.schema.ServerState
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ManagedServer(
    val id: String,
    val pterodactylId: Int?,
    val nodeId: String,
    val groupId: String,
    val templateVersionId: String,
    val host: String,
    val port: Int,
    val state: ServerState,
    val players: Int,
    val metadata: String?,
    val lastHeartbeat: String?,
    val stateChangedAt: String,
    val createdAt: String
)

object ServerService {

    fun getAll(groupId: String? = null, state: ServerState? = null): List<ManagedServer> = transaction {
        var query = Servers.selectAll()
        if (groupId != null) query = query.where { Servers.groupId eq groupId }
        if (state != null) query = query.where { Servers.state eq state.name }
        query.map { it.toManagedServer() }
    }

    fun getById(id: String): ManagedServer? = transaction {
        Servers.selectAll().where { Servers.id eq id }.map { it.toManagedServer() }.singleOrNull()
    }

    fun heartbeat(id: String, playerCount: Int, metadata: String? = null) = transaction {
        Servers.update({ Servers.id eq id }) {
            it[players] = playerCount
            if (metadata != null) it[Servers.metadata] = metadata
            it[lastHeartbeat] = LocalDateTime.now()
        }
    }

    fun updateState(id: String, newState: ServerState) = transaction {
        val current = Servers.selectAll().where { Servers.id eq id }.singleOrNull() ?: return@transaction
        val currentState = ServerState.fromString(current[Servers.state])
        
        // Basic validation: once terminated, stay terminated
        if (currentState == ServerState.TERMINATED) return@transaction

        Servers.update({ Servers.id eq id }) {
            it[state] = newState.name
            it[stateChangedAt] = LocalDateTime.now()
        }
        
        val s = current.toManagedServer()
        val redisKey = "stratus:group:${s.groupId}:ready"
        val serverKey = "stratus:server:$id"

        // Update Redis Cache
        if (newState == ServerState.READY) {
            RedisService.resource { jedis ->
                jedis.sadd(redisKey, id)
                jedis.hset(serverKey, mapOf(
                    "id" to s.id,
                    "host" to s.host,
                    "port" to s.port.toString(),
                    "templateVersion" to s.templateVersionId
                ))
                // Cache server details for 1 hour of inactivity
                jedis.expire(serverKey, 3600)
            }
        } else if (currentState == ServerState.READY && newState != ServerState.READY) {
            RedisService.resource { it.srem(redisKey, id) }
        }

        if (newState == ServerState.TERMINATED) {
            RedisService.resource { 
                it.srem(redisKey, id) 
                it.del(serverKey)
            }
        }

        // Publish event to Redis
        RedisService.publish("stratus:server:state", "{\"serverId\":\"$id\", \"state\":\"${newState.name}\"}")
        
        if (newState == ServerState.READY) {
            val s = current.toManagedServer()
            RedisService.publish("stratus:server:ready", "{\"serverId\":\"$id\", \"groupId\":\"${s.groupId}\", \"host\":\"${s.host}\", \"port\":${s.port}}")
        } else if (newState == ServerState.TERMINATED) {
            val s = current.toManagedServer()
            RedisService.publish("stratus:server:removed", "{\"serverId\":\"$id\", \"groupId\":\"${s.groupId}\"}")
        }
    }

    fun create(
        id: String = UUID.randomUUID().toString(),
        pteroId: Int?,
        nodeId: String,
        groupId: String,
        templateVersionId: String,
        host: String,
        port: Int
    ): ManagedServer = transaction {
        Servers.insert {
            it[Servers.id] = id
            it[Servers.pterodactylId] = pteroId
            it[Servers.nodeId] = nodeId
            it[Servers.groupId] = groupId
            it[Servers.templateVersionId] = templateVersionId
            it[Servers.host] = host
            it[Servers.port] = port
            it[Servers.state] = ServerState.STARTING.name
            it[Servers.stateChangedAt] = LocalDateTime.now()
            it[Servers.createdAt] = LocalDateTime.now()
        }
        ManagedServer(id, pteroId, nodeId, groupId, templateVersionId, host, port, ServerState.STARTING, 0, null, null, LocalDateTime.now().toString(), LocalDateTime.now().toString())
    }

    fun createWithId(
        id: String,
        pteroId: Int?,
        nodeId: String,
        groupId: String,
        templateVersionId: String,
        host: String,
        port: Int
    ) = create(id, pteroId, nodeId, groupId, templateVersionId, host, port)

    fun delete(id: String) = transaction {
        Servers.deleteWhere { Servers.id eq id }
    }

    fun getStuckServers(startingTimeout: LocalDateTime, heartbeatTimeout: LocalDateTime): List<ManagedServer> = transaction {
        val stuckStarting = Servers.selectAll().where { 
            (Servers.state eq ServerState.STARTING.name) and (Servers.createdAt less startingTimeout) 
        }
        
        val missingHeartbeat = Servers.selectAll().where {
            (Servers.state notInList ServerState.TERMINAL.map { it.name }) and 
            (Servers.lastHeartbeat less heartbeatTimeout)
        }
        
        (stuckStarting.map { it.toManagedServer() } + missingHeartbeat.map { it.toManagedServer() }).distinctBy { it.id }
    }

    private fun ResultRow.toManagedServer() = ManagedServer(
        id = this[Servers.id],
        pterodactylId = this[Servers.pterodactylId],
        nodeId = this[Servers.nodeId],
        groupId = this[Servers.groupId],
        templateVersionId = this[Servers.templateVersionId],
        host = this[Servers.host],
        port = this[Servers.port],
        state = ServerState.fromString(this[Servers.state]),
        players = this[Servers.players],
        metadata = this[Servers.metadata],
        lastHeartbeat = this[Servers.lastHeartbeat]?.toString(),
        stateChangedAt = this[Servers.stateChangedAt].toString(),
        createdAt = this[Servers.createdAt].toString()
    )
}
