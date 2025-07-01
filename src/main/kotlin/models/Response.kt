package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    val id: Int? = null,
    val email: String?= null,
    val name: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,

)