package models

import kotlinx.serialization.Serializable

@Serializable
data class AuthCheckResponse(
    val isAuthenticated: Boolean,
    val userId: Int? = null,

) {
    companion object {
        fun unauthenticated(): AuthCheckResponse {
            return AuthCheckResponse(isAuthenticated = false)
        }

        fun authenticated(userId: Int, email: String, name: String?): AuthCheckResponse {
            return AuthCheckResponse(isAuthenticated = true, userId = userId)
        }
    }
}