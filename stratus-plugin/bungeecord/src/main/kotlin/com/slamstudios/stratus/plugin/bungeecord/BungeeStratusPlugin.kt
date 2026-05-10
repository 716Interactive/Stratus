package com.slamstudios.stratus.plugin.bungeecord

import com.slamstudios.stratus.plugin.common.StratusPluginBase
import net.md_5.bungee.api.plugin.Plugin

class BungeeStratusPlugin : Plugin() {
    private val impl = object : StratusPluginBase() {
        override fun getPlayerCount(): Int = proxy.onlineCount
        override fun getMetadata(): String? = "{\"platform\":\"bungeecord\"}"
    }

    override fun onEnable() {
        impl.onEnable()
    }

    override fun onDisable() {
        impl.onDisable()
    }
}
