package com.slamstudios.stratus.plugin.velocity

import com.google.inject.Inject
import com.slamstudios.stratus.plugin.common.StratusPluginBase
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import kotlinx.serialization.json.*
import redis.clients.jedis.JedisPubSub
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

@Plugin(id = "stratus", name = "Stratus", version = "1.0", authors = ["SlamTheHam"])
class VelocityStratusPlugin @Inject constructor(
    private val server: ProxyServer
) : StratusPluginBase() {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        super.onEnable()
        
        // Start Redis listener for dynamic server registration
        startRedisListener()
    }

    private fun startRedisListener() {
        scope.launch {
            val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
            val redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379
            
            val jedis = redis.clients.jedis.Jedis(redisHost, redisPort)
            
            jedis.subscribe(object : JedisPubSub() {
                override fun onMessage(channel: String, message: String) {
                    val json = Json.parseToJsonElement(message).jsonObject
                    
                    when (channel) {
                        "stratus:server:ready" -> {
                            val name = json["serverId"]?.jsonPrimitive?.content ?: return
                            val host = json["host"]?.jsonPrimitive?.content ?: return
                            val port = json["port"]?.jsonPrimitive?.int ?: return
                            
                            val info = ServerInfo(name, InetSocketAddress(host, port))
                            server.registerServer(info)
                            logger.info("Dynamically registered server $name ($host:$port)")
                        }
                        "stratus:server:removed" -> {
                            val name = json["serverId"]?.jsonPrimitive?.content ?: return
                            server.getServer(name).ifPresent {
                                server.unregisterServer(it.serverInfo)
                                logger.info("Unregistered server $name")
                            }
                        }
                    }
                }
            }, "stratus:server:ready", "stratus:server:removed")
        }
    }

    override fun getPlayerCount(): Int = server.playerCount
    override fun getMetadata(): String? = "{\"platform\":\"velocity\"}"
}
