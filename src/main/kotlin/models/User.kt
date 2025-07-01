package models

import org.jetbrains.exposed.sql.Table
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime


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
@Serializable
data class LogoutRequest(
    val refreshToken: String
)

object UserAddress : Table("addresses") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val addressType = varchar("address_type", 50).nullable() // e.g., "billing", "shipping"
    val addressLine1 = varchar("address_line1", 255).nullable()
    val addressLine2 = varchar("address_line2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val state = varchar("state", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 100).nullable()
    override val primaryKey = PrimaryKey(id)

    init {
        // Index for faster lookups
        index(false, userId)
        index(false, city)
        index(false, country)
    }
}

object UserDetails : Table("user_details") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val phoneNumber = varchar("phone_number", 20).nullable()
    val dateOfBirth = text("date_of_birth").nullable()
    val profilePictureUrl = text("profile_picture_url").nullable()
    val lastLogin = datetime("last_login").nullable()

    override val primaryKey = PrimaryKey(id)
}

object UserPreferences : Table("user_preferences") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val preferredPaymentMethod = varchar("preferred_payment_method", 50).nullable()
    val shippingSameAsBilling = bool("shipping_same_as_billing").default(true)

    override val primaryKey = PrimaryKey(id)

    init {
        // Create index for optimized querying
        index(false, userId)
    }
}

@Serializable
data class UserPreferencesRequest(
    val userId: Int,
    val preferredPaymentMethod: String? = null,
    val shippingSameAsBilling: Boolean = true
)