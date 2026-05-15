package com.slamstudios.stratus.services

import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

@Serializable
data class FileEntry(
    val name: String,
    val size: Long,
    val isFile: Boolean,
    val isDirectory: Boolean,
    val isEditable: Boolean,
    val mimetype: String,
    val modifiedAt: String
)

object FileService {
    private val logger = LoggerFactory.getLogger(FileService::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))

    fun listFiles(basePath: String, subPath: String): List<FileEntry> {
        val safePath = subPath.trimStart('/')
        val root = File(basePath, safePath).canonicalFile
        
        // Security check: Ensure we stay within the template directory
        val baseDir = File(basePath).canonicalFile
        if (!root.absolutePath.startsWith(baseDir.absolutePath)) {
            logger.warn("Security alert: Attempted path escape to $root")
            return emptyList()
        }

        if (!root.exists()) {
            root.mkdirs()
        }
        
        return root.listFiles()?.map { file ->
            val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            FileEntry(
                name = file.name,
                size = attrs.size(),
                isFile = attrs.isRegularFile,
                isDirectory = attrs.isDirectory,
                isEditable = isEditable(file),
                mimetype = probeContentType(file),
                modifiedAt = dateTimeFormatter.format(attrs.lastModifiedTime().toInstant())
            )
        } ?: emptyList()
    }

    private fun isEditable(file: File): Boolean {
        if (!file.isFile) return false
        val ext = file.extension.lowercase()
        return listOf("txt", "yml", "yaml", "json", "conf", "config", "sh", "properties", "xml", "log").contains(ext)
    }

    private fun probeContentType(file: File): String {
        return Files.probeContentType(file.toPath()) ?: "application/octet-stream"
    }

    fun getFileContents(basePath: String, subPath: String): String {
        val file = File(basePath, subPath.trimStart('/')).canonicalFile
        validatePath(basePath, file)
        return file.readText()
    }

    fun writeFileContents(basePath: String, subPath: String, content: String) {
        val file = File(basePath, subPath.trimStart('/')).canonicalFile
        validatePath(basePath, file)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    fun deleteFiles(basePath: String, subPaths: List<String>) {
        subPaths.forEach { subPath ->
            val file = File(basePath, subPath.trimStart('/')).canonicalFile
            validatePath(basePath, file)
            if (file.exists()) {
                file.deleteRecursively()
            }
        }
    }

    private fun validatePath(basePath: String, target: File) {
        val baseDir = File(basePath).canonicalFile
        if (!target.absolutePath.startsWith(baseDir.absolutePath)) {
            throw SecurityException("Illegal file access: $target")
        }
    }
}
