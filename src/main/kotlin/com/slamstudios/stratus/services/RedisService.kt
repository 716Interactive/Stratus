package com.slamstudios.stratus.services

import com.slamstudios.stratus.config.RedisConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisService {
    private val logger = LoggerFactory.getLogger(RedisService::class.java)
    private var pool: JedisPool? = null

    fun init(config: RedisConfig) {
        logger.info("Connecting to Redis at ${config.host}:${config.port}…")
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = 16
            maxIdle = 8
            minIdle = 2
        }
        
        pool = if (config.password != null) {
            JedisPool(poolConfig, config.host, config.port, 2000, config.password)
        } else {
            JedisPool(poolConfig, config.host, config.port, 2000)
        }

        // Test connection
        try {
            resource { it.ping() }
            logger.info("Redis connected successfully.")
        } catch (e: Exception) {
            logger.error("Failed to connect to Redis: ${e.message}")
            throw e
        }
    }

    fun <T> resource(block: (Jedis) -> T): T {
        val jedis = pool?.resource ?: throw IllegalStateException("Redis pool not initialised")
        return try {
            block(jedis)
        } finally {
            jedis.close()
        }
    }

    fun publish(channel: String, message: String) {
        resource { it.publish(channel, message) }
    }
}
