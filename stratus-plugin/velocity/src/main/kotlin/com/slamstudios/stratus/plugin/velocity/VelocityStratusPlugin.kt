package com.slamstudios.stratus.plugin.velocity

import com.google.inject.Inject
import com.slamstudios.stratus.plugin.common.StratusPluginBase
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import kotlinx.serialization.json.*
import redis.clients.jedis.JedisPubSub
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

import java.nio.file.Path
import com.velocitypowered.api.plugin.annotation.DataDirectory
import java.io.File
import java.util.UUID

@Plugin(id = "stratus", name = "Stratus", version = "1.0", authors = ["SlamTheHam"])
class VelocityStratusPlugin @Inject constructor(
    private val server: ProxyServer,
    @DataDirectory private val dataDirectory: Path
) : StratusPluginBase() {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        super.onEnable()
        
        // Start Redis listener for dynamic server registration
        startRedisListener()
        
        // Dynamically register proxy with its generated token
        registerProxyInstance()
    }

    private fun registerProxyInstance() {
        scope.launch {
            delay(3000) // Allow Ktor API client to initialize
            try {
                val dir = dataDirectory.toFile()
                if (!dir.exists()) dir.mkdirs()
                
                val configFile = File(dir, "config.yml")
                var token = ""
                
                if (configFile.exists()) {
                    val lines = configFile.readLines()
                    val tokenLine = lines.find { it.startsWith("token:") }
                    if (tokenLine != null) {
                        token = tokenLine.substringAfter("token:").trim().replace("\"", "").replace("'", "")
                    }
                }
                
                if (token.isBlank()) {
                    token = UUID.randomUUID().toString().replace("-", "")
                    configFile.writeText("token: \"$token\"\n")
                    logger.info("Generated new secure proxy token: $token")
                } else {
                    logger.info("Loaded existing proxy token from config.")
                }
                
                val address = server.boundAddress
                val host = address?.hostString ?: System.getenv("PROXY_HOST") ?: "127.0.0.1"
                val port = address?.port ?: System.getenv("PROXY_PORT")?.toInt() ?: 25577
                
                val registered = api?.registerProxy("Velocity", host, port, true, token)
                if (registered == true) {
                    logger.info("Successfully registered Velocity proxy ($host:$port) with token in Orchestrator.")
                } else {
                    logger.warn("Orchestrator proxy registration failed. Using standalone mode.")
                }
            } catch (e: Exception) {
                logger.error("Error during Velocity proxy auto-registration: ${e.message}")
            }
        }
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

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        super.shutdown()
    }
}
