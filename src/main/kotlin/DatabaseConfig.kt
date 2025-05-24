// src/main/kotlin/com/example/DatabaseConfig.kt
package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.ConnectException

object DatabaseFactory {
    fun init() {
        try {
            val config = HikariConfig().apply {
                jdbcUrl = buildSupabaseUrl()
                username = System.getenv("DB_USER") ?: throw IllegalStateException("DB_USER required")
                password = System.getenv("DB_PASSWORD") ?: throw IllegalStateException("DB_PASSWORD required")
                maximumPoolSize = 3
                connectionTimeout = 30000
                addDataSourceProperty("sslmode", "require") // SSL enabled
            }


            Database.connect(HikariDataSource(config))

            // Test connection
            transaction {
                exec("SELECT 1") {}
                println("✅ Database connection successful")
            }
        } catch (e: Exception) {
            System.err.println("❌ Database connection failed: ${e.message}")
            throw e
        }
    }

    private fun buildSupabaseUrl(): String {
        val baseUrl = System.getenv("DB_URL") ?: throw IllegalStateException("DB_URL required")
        return if (baseUrl.contains("?")) {
            "$baseUrl&sslmode=require"
        } else {
            "$baseUrl?sslmode=require"
        }
    }
}