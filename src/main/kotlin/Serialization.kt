package com.example

import AnySerializer
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true    // Makes JSON output human-readable
            isLenient = true      // Allows malformed JSON
            ignoreUnknownKeys = true // Skips unknown JSON fields
            explicitNulls = false // Omits null values from serialization
            coerceInputValues = true // Handles default values for missing fields
            serializersModule = SerializersModule {
                // This registers serializers for kotlinx.datetime types
                contextual(LocalDate.serializer())
            }
        })
    }
    routing {
        get("/json/kotlinx-serialization") {
                call.respond(mapOf("hello" to "world"))
            }
    }
}
