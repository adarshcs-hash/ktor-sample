package com.example

import AuthenticationException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.RefreshTokens
import com.example.models.RegisterResponse
import extensions.toUser
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.*
import models.responses.AccessTokenResponse
import models.responses.LoginResponse
import org.mindrot.jbcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Period
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

import java.util.*


data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)
fun Application.configureAuth() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: throw IllegalStateException("JWT_SECRET not found")
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: throw IllegalStateException("JWT_ISSUER not found")
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: throw IllegalStateException("JWT_AUDIENCE not found")
    val jwtRealm = System.getenv("JWT_REALM") ?: throw IllegalStateException("JWT_REALM not found")

    // JWT Configuration
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                credential.payload.getClaim("userId").asInt()?.let { userId ->
                    JWTPrincipal(credential.payload)
                }
            }
        }
    }

    routing {
        route("/auth") {
            // Registration Endpoint
            post("/register") {
                val newUser = call.receive<NewUser>()
                val email = newUser.email.trim().lowercase()

                val userExists = transaction {
                    Users.selectAll().where { Users.email.lowerCase() eq email }.count() > 0
                }

                if (userExists) {
                    call.respond(
                        HttpStatusCode.Conflict, mapOf(
                            "error" to "Email already registered",
                            "email" to email
                        )
                    )
                    return@post
                }

                val id = transaction {
                    Users.insert {
                        it[Users.email] = email
                        it[passwordHash] = BCrypt.hashpw(newUser.password, BCrypt.gensalt())
                        it[name] = newUser.name
                    }[Users.id]
                }

                call.respond(HttpStatusCode.Created, RegisterResponse(id, email))
            }

            // Login Endpoint
            // AuthRoutes.kt
            post("/login") {
                val credentials = call.receive<LoginRequest>().also {
                    // Input validation
                    if (it.email.isBlank() || it.password.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email and password are required"))
                        return@post
                    }
                }

                val email = credentials.email.trim().lowercase()
                val user = try {
                    transaction {
                        Users.selectAll().where { Users.email eq email }
                            .limit(1)
                            .map { it.toUser() }
                            .firstOrNull()
                    } ?: run {
                        // Simulate password check for timing attack protection
                        BCrypt.hashpw("dummy", BCrypt.gensalt())
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                        return@post
                    }
                } catch (e: Exception) {
                    application.log.error("Database error", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Service unavailable"))
                    return@post
                }

                if (!BCrypt.checkpw(credentials.password, user.passwordHash)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                    return@post
                }
                // Generate tokens
                val (accessToken, refreshToken) = try {
                    Pair(
                        TokenManager.generateAccessToken(user.id, user.email),
                        TokenManager.generateRefreshToken()
                    )
                } catch (e: Exception) {
                    application.log.error("Token generation failed", e)
                    throw AuthenticationException("Service unavailable")
                }

                // Store refresh token
                try {
                    transaction {
                        RefreshTokens.insert {
                            it[userId] = user.id
                            it[token] = refreshToken
                            it[expiresAt] = LocalDateTime.now().plusMonths(1)
                            it[deviceInfo] = call.request.headers["User-Agent"]?.take(255)
                            it[createdAt] = LocalDateTime.now()
                        }
                    }
                } catch (e: Exception) {
                    application.log.error("Failed to store refresh token", e)
                    throw AuthenticationException("Service unavailable")
                }

                call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                     token_type = "Bearer",
                        access_token = accessToken,
                        refresh_token = refreshToken,
                        expires_in = Period.ofDays(30).toTotalMonths().toLong() * 24 * 60 * 60
                    ))
            }

            post("/refresh") {
                val refreshTokenRequest = call.receive<RefreshTokenRequest>()

                // Validate the refresh token
                val refreshToken = refreshTokenRequest.refreshToken
                val userId = transaction {
                    RefreshTokens.selectAll().where { RefreshTokens.token eq refreshToken }
                        .map { it[RefreshTokens.userId] }
                        .firstOrNull()
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
                    return@post
                }

                // Check if the refresh token is expired
                val tokenData = transaction {
                    RefreshTokens.selectAll().where { RefreshTokens.token eq refreshToken }
                        .map { it[RefreshTokens.expiresAt] }
                        .firstOrNull()
                }

                if (tokenData == null || tokenData.isBefore(LocalDateTime.now())) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Refresh token expired"))
                    return@post
                }

                // Generate new access token
                val user = transaction {
                    Users.selectAll().where { Users.id eq userId }
                        .map { it.toUser () }
                        .firstOrNull()
                } ?: run {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "User  not found"))
                    return@post
                }

                val newAccessToken = TokenManager.generateAccessToken(user.id, user.email)

                // Respond with new access token
                call.respond(
                    HttpStatusCode.OK,
                    AccessTokenResponse(
                        token_type = "Bearer",
                        access_token = newAccessToken,
                        expires_in = Period.ofDays(30).toTotalMonths() * 24 * 60 * 60
                    )
                )
            }
        }

        // Refresh Token Endpoint



        // Protected Routes
        authenticate("auth-jwt") {
            get("/protected") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()
                val email = principal.payload.getClaim("email").asString()

                call.respond(
                    mapOf(
                        "message" to "Authenticated successfully",
                        "userId" to userId,
                        "email" to email
                    )
                )
            }
        }
    }
}