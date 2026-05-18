package com.slamstudios.stratus.db

import com.slamstudios.stratus.config.DatabaseConfig
import com.slamstudios.stratus.db.schema.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init(config: DatabaseConfig) {
        val dataSource = buildDataSource(config)

        // ── Run Flyway migrations ─────────────────────────────────────────────
        log.info("Running Flyway migrations…")
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateMigrationNaming(true)
            .load()

        val result = flyway.migrate()
        log.info("Flyway applied ${result.migrationsExecuted} migration(s).")

        // ── Connect Exposed ───────────────────────────────────────────────────
        Database.connect(dataSource)
        log.info("Database connected: ${config.url}")

        // ── Migrate existing templates path in database & filesystem ──────────
        transaction {
            Templates.update {
                it[localPath] = "/var/lib/pterodactyl/volumes"
            }
            
            // Check filesystem and migrate physically
            Templates.selectAll().forEach { row ->
                val templateId = row[Templates.id]
                val oldDir = java.io.File("/var/lib/pterodactyl/templates/$templateId")
                val newDir = java.io.File("/var/lib/pterodactyl/volumes/$templateId")
                
                if (oldDir.exists() && oldDir.isDirectory) {
                    log.info("Migrating template physical directory from $oldDir to $newDir…")
                    try {
                        if (!newDir.exists()) {
                            newDir.mkdirs()
                        }
                        oldDir.copyRecursively(newDir, overwrite = true)
                        oldDir.deleteRecursively()
                        log.info("Successfully migrated template physical directory $templateId.")
                    } catch (e: Exception) {
                        log.error("Failed to physically migrate template $templateId: ${e.message}", e)
                    }
                }
            }
        }

        // ── Prune stale terminated servers on startup ─────────────────────────
        transaction {
            Servers.deleteWhere { Servers.state eq ServerState.TERMINATED.name }
        }
        log.info("Pruned all stale terminated servers from database on startup.")
    }

    private fun buildDataSource(config: DatabaseConfig): HikariDataSource {
        val hikari = HikariConfig().apply {
            jdbcUrl         = config.url
            username        = config.user
            password        = config.password
            maximumPoolSize = config.poolSize
            minimumIdle     = 2
            idleTimeout     = 600_000
            connectionTimeout = 30_000
            driverClassName = "org.mariadb.jdbc.Driver"
            poolName        = "StratusPool"
            // MariaDB-specific keep-alive
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        return HikariDataSource(hikari)
    }
}
