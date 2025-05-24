package com.example


import io.github.cdimascio.dotenv.dotenv
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

fun connectToDatabase() {
    val dotenv = dotenv()
    val config = HikariConfig().apply {
        jdbcUrl = dotenv["DB_URL"] ?: throw IllegalStateException("DB_URL not found in .env")
        driverClassName = "org.postgresql.Driver"
        username = dotenv["DB_USER"] ?: throw IllegalStateException("DB_USER not found in .env")
        password = dotenv["DB_PASSWORD"] ?: throw IllegalStateException("DB_PASSWORD not found in .env")
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
}