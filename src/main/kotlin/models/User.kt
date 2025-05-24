package models

import org.jetbrains.exposed.sql.Table
import kotlinx.serialization.Serializable


object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 255).nullable()

    // Explicitly define additional indexes
    init {
        index(isUnique = false, columns = arrayOf(email))  // Non-unique for faster searches
    }

    override val primaryKey = PrimaryKey(id)
}
@Serializable
data class User(val id: Int, val email: String, val passwordHash: String, val name: String?)
@Serializable
data class NewUser(val email: String, val password: String, val name: String?)
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)