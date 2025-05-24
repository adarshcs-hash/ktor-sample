package com.example.models

import models.Users
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object RefreshTokens : Table("refresh_tokens") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val revoked = bool("revoked").default(false)
    val deviceInfo = text("device_info").nullable()  // Optional

    override val primaryKey = PrimaryKey(id)

    init {
        // Create additional indexes through Exposed
        index(false, userId)
        index(false, expiresAt)
    }
}
