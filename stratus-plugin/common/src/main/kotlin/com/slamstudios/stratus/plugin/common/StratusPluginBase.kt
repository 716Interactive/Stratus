package com.slamstudios.stratus.plugin.common

import com.slamstudios.stratus.api.StratusAPI
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

abstract class StratusPluginBase {
    protected val logger = LoggerFactory.getLogger(this::class.java)
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    protected var api: com.slamstudios.stratus.api.StratusClient? = null

    open fun onEnable() {
        try {
            api = StratusAPI.get()
            logger.info("Stratus integration initialized.")
            
            scope.launch {
                api?.updateState("READY")
            }
            
            startHeartbeat()
        } catch (e: Exception) {
            logger.warn("Stratus environment not detected. Network features disabled: ${e.message}")
        }
    }

    private fun startHeartbeat() {
        scope.launch {
            while (isActive) {
                try {
                    val players = getPlayerCount()
                    api?.heartbeat(players, getMetadata())
                } catch (e: Exception) {
                    logger.error("Heartbeat failure: ${e.message}")
                }
                delay(30_000)
            }
        }
    }

    open fun shutdown() {
        runBlocking {
            try {
                api?.updateState("TERMINATED")
            } catch (ignore: Exception) {}
        }
        scope.cancel()
    }

    abstract fun getPlayerCount(): Int
    abstract fun getMetadata(): String?
}
