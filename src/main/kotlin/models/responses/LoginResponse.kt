package models.responses

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val token_type: String,
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long
)
