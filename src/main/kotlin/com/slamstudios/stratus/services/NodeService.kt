package com.slamstudios.stratus.services

import com.slamstudios.stratus.db.schema.Nodes
import com.slamstudios.stratus.db.schema.ServerState
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@Serializable
data class Node(
    val id: String,
    val pterodactylId: Int,
    val name: String,
    val host: String,
    val token: String,
    val totalMemory: Int,
    val totalDisk: Int,
    val allocatedMemory: Int = 0,
    val allocatedDisk: Int = 0
)

fun Node.usedMemory(): Int = if (allocatedMemory > 0) allocatedMemory else ServerService.getAll().filter { it.nodeId == this.id && it.state != ServerState.TERMINATED }.sumOf { it.memory }
fun Node.usedDisk(): Int = if (allocatedDisk > 0) allocatedDisk else ServerService.getAll().filter { it.nodeId == this.id && it.state != ServerState.TERMINATED }.sumOf { it.disk }
fun Node.canFit(memory: Int, disk: Int): Boolean = (totalMemory - usedMemory() >= memory) && (totalDisk - usedDisk() >= disk)

object NodeService {

    fun getAll(): List<Node> = transaction {
        Nodes.selectAll().map { it.toNode() }
    }

    fun getById(id: String): Node? = transaction {
        Nodes.selectAll().where { Nodes.id eq id }.map { it.toNode() }.singleOrNull()
    }

    fun getByPterodactylId(pterodactylId: Int): Node? = transaction {
        Nodes.selectAll().where { Nodes.pterodactylId eq pterodactylId }.map { it.toNode() }.singleOrNull()
    }

    fun create(node: Node): Node = transaction {
        Nodes.insert {
            it[id] = node.id
            it[pterodactylId] = node.pterodactylId
            it[name] = node.name
            it[host] = node.host
            it[token] = node.token
            it[totalMemory] = node.totalMemory
            it[totalDisk] = node.totalDisk
            it[allocatedMemory] = node.allocatedMemory
            it[allocatedDisk] = node.allocatedDisk
        }
        node
    }

    fun update(id: String, name: String, memory: Int, disk: Int, allocatedMemory: Int, allocatedDisk: Int) = transaction {
        Nodes.update({ Nodes.id eq id }) {
            it[Nodes.name] = name
            it[Nodes.totalMemory] = memory
            it[Nodes.totalDisk] = disk
            it[Nodes.allocatedMemory] = allocatedMemory
            it[Nodes.allocatedDisk] = allocatedDisk
        }
    }

    fun delete(id: String) = transaction {
        Nodes.deleteWhere { Nodes.id eq id }
    }

    private fun ResultRow.toNode() = Node(
        id = this[Nodes.id],
        pterodactylId = this[Nodes.pterodactylId],
        name = this[Nodes.name],
        host = this[Nodes.host],
        token = this[Nodes.token],
        totalMemory = this[Nodes.totalMemory],
        totalDisk = this[Nodes.totalDisk],
        allocatedMemory = this[Nodes.allocatedMemory],
        allocatedDisk = this[Nodes.allocatedDisk]
    )
}
