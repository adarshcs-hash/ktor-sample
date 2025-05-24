package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(
    val id: Int,
    val email: String
)