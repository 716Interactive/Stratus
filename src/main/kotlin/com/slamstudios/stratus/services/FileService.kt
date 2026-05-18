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

    fun saveFile(basePath: String, subPath: String, inputStream: java.io.InputStream) {
        val file = File(basePath, subPath.trimStart('/')).canonicalFile
        validatePath(basePath, file)
        file.parentFile.mkdirs()
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
    }

    fun renameFile(basePath: String, oldPath: String, newPath: String) {
        val oldFile = File(basePath, oldPath.trimStart('/')).canonicalFile
        val newFile = File(basePath, newPath.trimStart('/')).canonicalFile
        validatePath(basePath, oldFile)
        validatePath(basePath, newFile)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
    }

    fun decompressFile(basePath: String, filePath: String) {
        val file = File(basePath, filePath.trimStart('/')).canonicalFile
        validatePath(basePath, file)
        if (file.exists() && file.name.endsWith(".zip", ignoreCase = true)) {
            val parentPath = file.parentFile.canonicalFile
            val parentRelative = parentPath.absolutePath.substring(File(basePath).canonicalFile.absolutePath.length)
            file.inputStream().use { input ->
                extractZip(basePath, parentRelative, input)
            }
        }
    }

    fun compressFiles(basePath: String, directory: String, files: List<String>): String {
        val dir = File(basePath, directory.trimStart('/')).canonicalFile
        validatePath(basePath, dir)
        val archiveName = "archive-${System.currentTimeMillis() / 1000}.zip"
        val archiveFile = File(dir, archiveName)
        
        java.io.FileOutputStream(archiveFile).use { fos ->
            java.util.zip.ZipOutputStream(fos).use { zos ->
                files.forEach { fileName ->
                    val fileToCompress = File(dir, fileName).canonicalFile
                    validatePath(basePath, fileToCompress)
                    compressRecursive(basePath, zos, fileToCompress, fileName)
                }
            }
        }
        return archiveName
    }

    private fun compressRecursive(basePath: String, zos: java.util.zip.ZipOutputStream, file: File, path: String) {
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            if (files.isEmpty()) {
                zos.putNextEntry(java.util.zip.ZipEntry("$path/"))
                zos.closeEntry()
            } else {
                files.forEach { subFile ->
                    compressRecursive(basePath, zos, subFile, "$path/${subFile.name}")
                }
            }
        } else {
            zos.putNextEntry(java.util.zip.ZipEntry(path))
            file.inputStream().use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    fun extractZip(basePath: String, subPath: String, inputStream: java.io.InputStream) {
        val targetDir = File(basePath, subPath.trimStart('/')).canonicalFile
        validatePath(basePath, targetDir)
        targetDir.mkdirs()

        val zipInput = java.util.zip.ZipInputStream(inputStream)
        var entry = zipInput.nextEntry
        while (entry != null) {
            val file = File(targetDir, entry.name).canonicalFile
            validatePath(basePath, file)
            
            if (entry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile.mkdirs()
                file.outputStream().use { output ->
                    zipInput.copyTo(output)
                }
            }
            zipInput.closeEntry()
            entry = zipInput.nextEntry
        }
    }

    private fun validatePath(basePath: String, target: File) {
        val baseDir = File(basePath).canonicalFile
        if (!target.absolutePath.startsWith(baseDir.absolutePath)) {
            throw SecurityException("Illegal file access: $target")
        }
    }
}
