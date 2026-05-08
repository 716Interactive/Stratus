package com.slamstudios.stratus.services

import com.slamstudios.stratus.db.schema.Nodes
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
    val totalMemory: Int,
    val totalDisk: Int
)

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
            it[totalMemory] = node.totalMemory
            it[totalDisk] = node.totalDisk
        }
        node
    }

    fun update(id: String, name: String, memory: Int, disk: Int) = transaction {
        Nodes.update({ Nodes.id eq id }) {
            it[Nodes.name] = name
            it[Nodes.totalMemory] = memory
            it[Nodes.totalDisk] = disk
        }
    }

    fun delete(id: String) = transaction {
        Nodes.deleteWhere { Nodes.id eq id }
    }

    private fun ResultRow.toNode() = Node(
        id = this[Nodes.id],
        pterodactylId = this[Nodes.pterodactylId],
        name = this[Nodes.name],
        totalMemory = this[Nodes.totalMemory],
        totalDisk = this[Nodes.totalDisk]
    )
}
