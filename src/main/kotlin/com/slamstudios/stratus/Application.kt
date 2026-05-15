package com.slamstudios.stratus

import com.slamstudios.stratus.config.AppConfig
import com.slamstudios.stratus.db.DatabaseFactory
import com.slamstudios.stratus.plugins.configureRouting
import com.slamstudios.stratus.plugins.configureSerialization
import com.slamstudios.stratus.plugins.configureStatusPages
import com.slamstudios.stratus.services.OrchestratorService
import com.slamstudios.stratus.services.PterodactylService
import com.slamstudios.stratus.services.RedisService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import org.slf4j.event.Level

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

fun main(args: Array<String>) {
    // Priority: /etc/stratus/config.yml > config.yml
    val yamlFile = listOf(
        File("/etc/stratus/config.yml"),
        File("config.yml")
    ).firstOrNull { it.exists() }

    if (yamlFile != null) {
        try {
            val yamlData = Yaml().load<Map<String, Any>>(FileInputStream(yamlFile)) ?: emptyMap()
            val port = yamlData["port"]?.toString()?.toIntOrNull()
            if (port != null) {
                // Inject into system properties so EngineMain picks it up via HOCON ${?PORT} or direct property
                System.setProperty("PORT", port.toString())
                System.setProperty("ktor.deployment.port", port.toString())
            }
        } catch (e: Exception) {
            System.err.println("[Stratus] Failed to pre-parse YAML port: ${e.message}")
        }
    }
    EngineMain.main(args)
}

fun Application.module() {
    println("[Stratus] --- Starting Stratus Orchestrator v1.0.1 ---")
    val config = AppConfig.load(environment.config)

    // ── Plugins ──────────────────────────────────────────────────────────────
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
    }

    // ── Database ─────────────────────────────────────────────────────────────
    DatabaseFactory.init(config.database)

    // ── Redis ────────────────────────────────────────────────────────────────
    RedisService.init(config.redis)

    // ── Pterodactyl ──────────────────────────────────────────────────────────
    PterodactylService.init(config.pterodactyl)

    // ── Orchestrator ─────────────────────────────────────────────────────────
    OrchestratorService.start(config)

    // ── Feature Plugins ───────────────────────────────────────────────────────
    configureSerialization()
    configureStatusPages()
    configureRouting(config)
}
