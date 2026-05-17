package com.slamstudios.stratus.plugin.spigot

import com.slamstudios.stratus.plugin.common.StratusPluginBase
import org.bukkit.plugin.java.JavaPlugin

import kotlinx.coroutines.*
import java.io.File

class SpigotStratusPlugin : JavaPlugin() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val impl = object : StratusPluginBase() {
        override fun getPlayerCount(): Int = server.onlinePlayers.size
        override fun getMetadata(): String? = "{\"platform\":\"spigot\"}"
    }

    override fun onEnable() {
        impl.onEnable()
        
        // Fetch proxy token and auto-configure BungeeGuard
        scope.launch {
            delay(2000) // Wait for network initialization
            try {
                val token = impl.api?.getProxyToken()
                if (!token.isNullOrBlank()) {
                    updateBungeeGuardConfig(token)
                }
            } catch (e: Exception) {
                impl.logger.warn("Failed to dynamically configure BungeeGuard: ${e.message}")
            }
        }
    }

    override fun onDisable() {
        impl.shutdown()
        scope.cancel()
    }

    private fun updateBungeeGuardConfig(token: String) {
        val bungeeGuardDir = File("plugins/BungeeGuard")
        val configFile = File(bungeeGuardDir, "config.yml")
        
        if (!bungeeGuardDir.exists()) {
            bungeeGuardDir.mkdirs()
        }

        if (!configFile.exists()) {
            configFile.writeText("""
                # BungeeGuard Configuration (Dynamic Stratus Provision)
                allowed-tokens:
                  - "$token"
            """.trimIndent())
            impl.logger.info("Generated new BungeeGuard config with dynamic proxy token.")
            return
        }

        val content = configFile.readText()
        if (!content.contains(token)) {
            val newContent = if (content.contains("allowed-tokens:")) {
                content.replace("allowed-tokens:", "allowed-tokens:\n  - \"$token\"")
            } else {
                content + "\nallowed-tokens:\n  - \"$token\""
            }
            configFile.writeText(newContent)
            impl.logger.info("Dynamically injected active proxy token into BungeeGuard allowed-tokens.")
        }
    }
}
