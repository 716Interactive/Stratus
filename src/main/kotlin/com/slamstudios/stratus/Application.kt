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

fun main(args: Array<String>) = EngineMain.main(args)

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
