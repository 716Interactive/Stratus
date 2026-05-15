package com.slamstudios.stratus.services

import com.slamstudios.stratus.db.schema.Templates
import com.slamstudios.stratus.db.schema.TemplateVersions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Template(
    val id: String,
    val name: String,
    val currentVersionId: String?,
    val localPath: String,
    val ownerId: Int
)

@Serializable
data class TemplateVersion(
    val id: String,
    val templateId: String,
    val versionNumber: Int,
    val eggId: Int,
    val configJson: String,
    val createdAt: String
)

object TemplateService {

    fun getAll(): List<Template> = transaction {
        Templates.selectAll().map { it.toTemplate() }
    }

    fun getById(id: String): Template? = transaction {
        Templates.selectAll().where { Templates.id eq id }.map { it.toTemplate() }.singleOrNull()
    }

    fun getVersions(templateId: String): List<TemplateVersion> = transaction {
        TemplateVersions.selectAll().where { TemplateVersions.templateId eq templateId }
            .orderBy(TemplateVersions.versionNumber, SortOrder.DESC)
            .map { it.toTemplateVersion() }
    }

    fun createTemplate(name: String, ownerId: Int = 1): Template = transaction {
        val id = UUID.randomUUID().toString()
        Templates.insert {
            it[Templates.id] = id
            it[Templates.name] = name
            it[Templates.localPath] = "/var/lib/pterodactyl/templates"
            it[Templates.ownerId] = ownerId
        }
        Template(id, name, null, "/var/lib/pterodactyl/templates", ownerId)
    }

    fun createVersion(templateId: String, eggId: Int, configJson: String): TemplateVersion = transaction {
        val lastVersion = TemplateVersions.selectAll().where { TemplateVersions.templateId eq templateId }
            .orderBy(TemplateVersions.versionNumber, SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.get(TemplateVersions.versionNumber) ?: 0
        
        val id = UUID.randomUUID().toString()
        val nextVersion = lastVersion + 1
        
        TemplateVersions.insert {
            it[TemplateVersions.id] = id
            it[TemplateVersions.templateId] = templateId
            it[TemplateVersions.versionNumber] = nextVersion
            it[TemplateVersions.eggId] = eggId
            it[TemplateVersions.configJson] = configJson
            it[TemplateVersions.createdAt] = LocalDateTime.now()
        }
        
        // Update current version pointer
        Templates.update({ Templates.id eq templateId }) {
            it[currentVersionId] = id
        }
        
        TemplateVersion(id, templateId, nextVersion, eggId, configJson, LocalDateTime.now().toString())
    }

    private fun ResultRow.toTemplate() = Template(
        id = this[Templates.id],
        name = this[Templates.name],
        currentVersionId = this[Templates.currentVersionId],
        localPath = this[Templates.localPath],
        ownerId = this[Templates.ownerId]
    )

    private fun ResultRow.toTemplateVersion() = TemplateVersion(
        id = this[TemplateVersions.id],
        templateId = this[TemplateVersions.templateId],
        versionNumber = this[TemplateVersions.versionNumber],
        eggId = this[TemplateVersions.eggId],
        configJson = this[TemplateVersions.configJson],
        createdAt = this[TemplateVersions.createdAt].toString()
    )
}
