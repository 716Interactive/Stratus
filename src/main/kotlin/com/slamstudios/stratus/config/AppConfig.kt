package com.slamstudios.stratus.config

import io.ktor.server.config.*

/**
 * Top-level application configuration loaded from application.conf.
 * All values can be overridden with environment variables (see application.conf).
 */
data class AppConfig(
    val token: String,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val pterodactyl: PterodactylConfig,
) {
    companion object {
        fun load(config: ApplicationConfig): AppConfig = AppConfig(
            token = config.property("stratus.token").getString(),
            database = DatabaseConfig(
                host     = config.property("stratus.database.host").getString(),
                port     = config.property("stratus.database.port").getString().toInt(),
                name     = config.property("stratus.database.name").getString(),
                user     = config.property("stratus.database.user").getString(),
                password = config.property("stratus.database.password").getString(),
                poolSize = config.property("stratus.database.poolSize").getString().toInt(),
            ),
            redis = RedisConfig(
                host     = config.property("stratus.redis.host").getString(),
                port     = config.property("stratus.redis.port").getString().toInt(),
                password = config.propertyOrNull("stratus.redis.password")?.getString()
                    ?.takeIf { it.isNotBlank() },
            ),
            pterodactyl = PterodactylConfig(
                baseUrl = config.property("stratus.pterodactyl.baseUrl").getString(),
                apiKey  = config.property("stratus.pterodactyl.apiKey").getString(),
                ownerId = config.property("stratus.pterodactyl.ownerId").getString().toInt(),
                orchestratorUrl = config.property("stratus.pterodactyl.orchestratorUrl").getString(),
            ),
        )
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
