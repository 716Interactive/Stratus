package com.slamstudios.stratus.plugin.spigot

import com.slamstudios.stratus.plugin.common.StratusPluginBase
import org.bukkit.plugin.java.JavaPlugin

class SpigotStratusPlugin : JavaPlugin() {
    private val impl = object : StratusPluginBase() {
        override fun getPlayerCount(): Int = server.onlinePlayers.size
        override fun getMetadata(): String? = "{\"platform\":\"spigot\"}"
    }

    override fun onEnable() {
        impl.onEnable()
    }

    override fun onDisable() {
        impl.onDisable()
    }
}
