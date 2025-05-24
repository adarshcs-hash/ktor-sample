package models.responses

import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenResponse(
    val token_type: String,
    val access_token: String,
    val expires_in: Long
)