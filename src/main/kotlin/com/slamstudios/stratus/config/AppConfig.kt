package com.slamstudios.stratus.config

import io.ktor.server.config.*
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

/**
 * Top-level application configuration.
 * Priority: 
 * 1. Environment variables
 * 2. config.yml
 * 3. application.conf (Ktor defaults)
 */
data class AppConfig(
    val port: Int,
    val token: String,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val pterodactyl: PterodactylConfig,
) {
    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            val log = org.slf4j.LoggerFactory.getLogger(AppConfig::class.java)
            
            val yamlFile = listOf(
                File("/etc/stratus/config.yml"),
                File("config.yml")
            ).firstOrNull { it.exists() }

            if (yamlFile != null) {
                println("[Stratus] Loading configuration from: ${yamlFile.absolutePath}")
                log.info("Loading configuration from: ${yamlFile.absolutePath}")
            } else {
                println("[Stratus] WARN: No YAML configuration file found!")
                log.warn("No YAML configuration file found! Using defaults and environment variables.")
            }

            val yamlData = if (yamlFile != null) {
                try {
                    Yaml().load<Map<String, Any>>(FileInputStream(yamlFile)) ?: emptyMap()
                } catch (e: Exception) {
                    println("[Stratus] ERROR: Failed to parse YAML: ${e.message}")
                    log.error("Failed to parse YAML configuration: ${e.message}")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            fun getProp(path: String, yamlPath: String): String? {
                // 1. Check YAML (User's manual config file)
                val yamlParts = yamlPath.split(".")
                var current: Any? = yamlData
                for (part in yamlParts) {
                    current = (current as? Map<*, *>)?.get(part)
                }
                if (current != null) return current.toString()

                // 2. Check environment variable
                val envVar = path.uppercase().replace(".", "_")
                System.getenv(envVar)?.let { return it }
                
                // 3. Fallback to application.conf
                return config.propertyOrNull(path)?.getString()
            }

            val dbHost = getProp("stratus.database.host", "database.host") ?: "localhost"
            val dbUser = getProp("stratus.database.user", "database.user") ?: "stratus"
            val dbPass = getProp("stratus.database.password", "database.password") ?: "changeme"
            
            log.info("Database Configuration: host=$dbHost, user=$dbUser")
            val serverPort = getProp("ktor.deployment.port", "port")?.toInt() ?: 8081

            return AppConfig(
                port = serverPort,
                token = getProp("stratus.token", "token") ?: "changeme",
                database = DatabaseConfig(
                    host     = dbHost,
                    port     = getProp("stratus.database.port", "database.port")?.toInt() ?: 3306,
                    name     = getProp("stratus.database.name", "database.name") ?: "stratus",
                    user     = dbUser,
                    password = dbPass,
                    poolSize = getProp("stratus.database.poolSize", "database.poolSize")?.toInt() ?: 10,
                ),
                redis = RedisConfig(
                    host     = getProp("stratus.redis.host", "redis.host") ?: "localhost",
                    port     = getProp("stratus.redis.port", "redis.port")?.toInt() ?: 6379,
                    password = getProp("stratus.redis.password", "redis.password")?.takeIf { it.isNotBlank() },
                ),
                pterodactyl = PterodactylConfig(
                    baseUrl = getProp("stratus.pterodactyl.baseUrl", "pterodactyl.baseUrl") ?: "http://localhost",
                    apiKey  = getProp("stratus.pterodactyl.apiKey", "pterodactyl.apiKey") ?: "changeme",
                    ownerId = getProp("stratus.pterodactyl.ownerId", "pterodactyl.ownerId")?.toInt() ?: 1,
                    orchestratorUrl = getProp("stratus.pterodactyl.orchestratorUrl", "pterodactyl.orchestratorUrl") ?: "http://localhost:8081",
                ),
            )
        }
    }
}

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val poolSize: Int,
) {
    val url: String get() = "jdbc:mariadb://$host:$port/$name"
}

data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String?,
)

data class PterodactylConfig(
    val baseUrl: String,
    val apiKey: String,
    val ownerId: Int,
    val orchestratorUrl: String,
)
