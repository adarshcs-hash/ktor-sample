package models.responses

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val userId: Int,
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
