package com.example

import ApiError
import ApiResponse
import AuthenticationException
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.RefreshTokens
import com.example.models.RegisterResponse
import extensions.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import ApiException
import AuthorizationException
import NotFoundException
import Products
import UserAddressResponse
import UserDetailsResponse
import UserProfileResponse
import UserResponse
import ValidationException
import io.github.jan.supabase.storage.storage
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import models.*
import models.responses.AccessTokenResponse
import models.responses.CartItemRequest
import models.responses.CartItemResponse
import models.responses.CartResponse
import models.responses.LoginResponse
import models.responses.ProductResponse
import models.responses.UserAddressRequest
import models.responses.UserDetailsRequest
import org.mindrot.jbcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import toUserAdditionalDetails
import toUserAddressRes
import toUserPreferencesRes
import toUserRes
import java.time.Period
import java.time.LocalDateTime
import java.util.UUID
import kotlin.and
import kotlin.text.set
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.toString

@OptIn(ExperimentalTime::class)
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
                println("Verifying token with audience: $jwtAudience, issuer: $jwtIssuer")
                println("Token claims: ${credential.payload.claims}")
                credential.payload.getClaim("email").asString()?.let { userId ->
                    JWTPrincipal(credential.payload)
                } ?: run {
                    println("Validation failed: Email claim missing")
                    null
                }
            }
            // respond for unauthorized requests
            challenge { _, _ ->
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = mapOf("message" to "The token provided is expired or invalid.")
                )
            }
        }
    }

    routing {
        route("/auth") {

            post("/register") {
                val newUser = call.receive<NewUser>()
                val email = newUser.email.trim().lowercase()

                val userExists = transaction {
                    Users.selectAll().where { Users.email.lowerCase() eq email }.count() > 0
                }

                if (userExists) {
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.Conflict.value,
                                validationErrors = mapOf("email" to "Email already registered"),
                                details = "Email already registered"
                            ),
                            message = "Email already registered"
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
                val accessToken = TokenManager.generateAccessToken(id, email)
                val refreshToken = TokenManager.generateRefreshToken()
                call.respond(
                    ApiResponse(
                        data = LoginResponse(
                            userId = id,
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            tokenType = TokenManager.TOKEN_TYPE,
                            expiresIn = TokenManager.ACCESS_TOKEN_EXPIRATION_SECONDS
                            ),
                        success = true,
                        message = "User registered successfully"
                    )
                )
            }

            post("/login") {
                try {
                    val credentials = call.receive<LoginRequest>().also {
                        val errors = mutableMapOf<String, String>()
                        if (it.email.isBlank()) errors["email"] = "Email is required"
                        if (it.password.isBlank()) errors["password"] = "Password is required"
                        if (errors.isNotEmpty()) {
                            call.respond(
                                ApiResponse<Unit>(
                                    success = false,
                                    error = ApiError(
                                        code = HttpStatusCode.BadRequest.value,
                                        validationErrors = errors,
                                        details = "Validation failed"
                                    ),
                                    message = "Invalid input"
                                )
                            )
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
                            BCrypt.hashpw("dummy", BCrypt.gensalt())
                            throw AuthenticationException("Invalid credentials")
                        }
                    } catch (e: Exception) {
                        call.respond(
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError(
                                    code = HttpStatusCode.InternalServerError.value,
                                    details = "Invalid credentials"
                                ),
                                message = "Invalid credentials"
                            )
                        )
                        return@post
                    }

                    if (!BCrypt.checkpw(credentials.password, user.passwordHash)) {
                        call.respond(
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError(
                                    code = HttpStatusCode.Unauthorized.value,
                                    details = "Invalid credentials"
                                ),
                                message = "Invalid credentials"
                            )
                        )
                    }

                    val (accessToken, refreshToken) = try {
                        Pair(
                            TokenManager.generateAccessToken(user.id, user.email),
                            TokenManager.generateRefreshToken()
                        )
                    } catch (e: Exception) {
                        throw ApiException(
                            HttpStatusCode.InternalServerError,
                            "Token generation failed",
                            details = e.message
                        )
                    }

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
                        throw ApiException(
                            HttpStatusCode.InternalServerError,
                            "Failed to store refresh token",
                            details = e.message
                        )
                    }

                    call.respond(
                        ApiResponse(
                            data = LoginResponse(
                                userId = user.id,
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                tokenType = TokenManager.TOKEN_TYPE,
                                expiresIn = TokenManager.ACCESS_TOKEN_EXPIRATION_SECONDS
                            ),
                            success = true,
                            message = "Login successful"
                        )

                    )

                } catch (e: ValidationException) {
                    call.respond(
                        message = e.message,
                        status = e.statusCode,
                    )
                } catch (e: AuthenticationException) {
                    call.respond(
                        message = e.message,
                        status = e.statusCode,
                    )
                } catch (e: AuthorizationException) {
                    call.respond(
                        message = e.message,
                        status = e.statusCode,
                    )
                } catch (e: ApiException) {
                    call.respond(
                        message = e.message,
                        status = e.statusCode,
                    )
                }
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
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.Unauthorized.value,
                                details = "Invalid refresh token"
                            ),
                            message = "Invalid refresh token"
                        )
                    )
                    return@post
                }


                val tokenData = transaction {
                    RefreshTokens.selectAll().where { RefreshTokens.token eq refreshToken }
                        .map { it[RefreshTokens.expiresAt] }
                        .firstOrNull()
                }

                if (tokenData == null || tokenData.isBefore(LocalDateTime.now())) {
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.Unauthorized.value,
                                details = "Refresh token expired"
                            ),
                            message = "Refresh token expired"
                        )
                    )
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Refresh token expired"))
                    return@post
                }


                val user = transaction {
                    Users.selectAll().where { Users.id eq userId }
                        .map { it.toUser() }
                        .firstOrNull()
                } ?: run {
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.NotFound.value,
                                details = "User not found"
                            ),
                            message = "User not found"
                        )
                    )
                    return@post
                }

                val newAccessToken = TokenManager.generateAccessToken(user.id, user.email)


                call.respond(
                    ApiResponse(
                        data = LoginResponse(
                            userId = user.id,
                            accessToken = newAccessToken,
                            refreshToken = refreshToken, // Keep the same refresh token
                            tokenType = TokenManager.TOKEN_TYPE,
                            expiresIn = TokenManager.ACCESS_TOKEN_EXPIRATION_SECONDS
                        ),
                        success = true,
                        message = "Access token refreshed successfully"
                    )

                )
            }

            post("/logout") {
                try {
                    val request = call.receive<LogoutRequest>()
                    val refreshToken = request.refreshToken

                    if (refreshToken.isBlank()) {
                        call.respond(
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError(
                                    code = HttpStatusCode.BadRequest.value,
                                    details = "Refresh token is required"
                                ),
                                message = "Invalid request"
                            )
                        )
                        return@post
                    }

                    val tokenRecord = transaction {
                        RefreshTokens.selectAll().where { RefreshTokens.token eq refreshToken }
                            .limit(1)
                            .firstOrNull()
                    }

                    if (tokenRecord == null) {
                        call.respond(
                            ApiResponse<Unit>(
                                success = false,
                                error = ApiError(
                                    code = HttpStatusCode.Unauthorized.value,
                                    details = "Invalid token"
                                ),
                                message = "Invalid token"
                            )
                        )
                        return@post
                    }


                    transaction {
                        RefreshTokens.deleteWhere { RefreshTokens.token eq refreshToken }
                    }
                    call.respond(
                        ApiResponse<Unit>(
                            success = true,
                            message = "Successfully logged out"
                        )
                    )
                } catch (e: ContentTransformationException) {
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.BadRequest.value,
                                details = "Invalid request format"
                            ),
                            message = "Invalid request format"
                        )
                    )

                } catch (e: Exception) {
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.InternalServerError.value,
                                details = "An error occurred while processing the request"
                            ),
                            message = "An error occurred while processing the request"
                        )
                    )
                }
            }

            get("/check-auth") {
                val refreshTokenRequest = call.receive<RefreshTokenRequest>()
                if (refreshTokenRequest.refreshToken.isBlank()) {
                    call.respond(
                        ApiResponse<Unit>(
                            success = false,
                            error = ApiError(
                                code = HttpStatusCode.BadRequest.value,
                                details = "Refresh token is required"
                            ),
                            message = "Invalid request"
                        )
                    )
                    return@get
                }
                val (isLoggedIn, userId) = try {
                    val result = transaction {
                        RefreshTokens.selectAll()
                            .where { RefreshTokens.token eq refreshTokenRequest.refreshToken }
                            .firstOrNull()
                            ?.let { tokenRecord ->
                                val isValid = tokenRecord[RefreshTokens.expiresAt].isAfter(LocalDateTime.now())
                                if (isValid) {
                                    Pair(true, tokenRecord[RefreshTokens.userId])
                                } else {
                                    Pair(false, null)
                                }
                            } ?: Pair(false, null)
                    }
                    result
                } catch (e: Exception) {
                    Pair(false, null)
                }

                call.respond(
                    ApiResponse(
                        success = isLoggedIn,
                        data = AuthCheckResponse(
                            isAuthenticated = isLoggedIn,
                            userId = userId
                        ),
                        message = if (isLoggedIn) "User is authenticated" else "User is not authenticated"
                    )
                )
            }


        }


        // Protected Routes
        authenticate("auth-jwt") {

            post("user/upload/profile-picture") {
                val multipart = call.receiveMultipart()
                var fileName: String? = null
                var fileBytes: ByteArray? = null
                var contentType: ContentType? = null


                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            fileName =
                                sanitizeFilename(part.originalFileName ?: "product_${System.currentTimeMillis()}")
                            fileBytes = part.streamProvider().readBytes()
                            contentType = part.contentType
                            part.dispose()
                        }

                        else -> part.dispose()
                    }
                }

                if (fileName == null || fileBytes == null) {
                    call.respond(
                        HttpStatusCode.BadRequest, mapOf(
                            "error" to "No file provided",
                            "status" to "failed"
                        )
                    )
                    return@post
                }

                if (!isValidImage(contentType)) {
                    call.respond(
                        HttpStatusCode.BadRequest, mapOf(
                            "error" to "Only JPEG, PNG, or WEBP images are allowed",
                            "status" to "failed"
                        )
                    )
                    return@post
                }

                if (fileBytes.size > 5_000_000) { // 5MB limit
                    call.respond(
                        HttpStatusCode.PayloadTooLarge, mapOf(
                            "error" to "File size exceeds 5MB limit",
                            "status" to "failed"
                        )
                    )
                    return@post
                }

                try {
                    val path = "images/${UUID.randomUUID()}_$fileName"
                    val storage = SupabaseClient.instance
                    storage.storage.from("products").upload(
                        path,
                        fileBytes,
                    )

                    val publicUrl = storage.storage.get("products").publicUrl(path)
                    transaction {
                      val v= UserDetails.selectAll().where{ UserDetails.userId eq call.principal<JWTPrincipal>()!!.payload.subject.toInt() }
                            .firstOrNull()?.let {
                                UserDetails.update({ UserDetails.userId eq call.principal<JWTPrincipal>()!!.payload.subject.toInt() }) {
                                    it[profilePictureUrl] = publicUrl
                                }
                            } ?: run {
                                UserDetails.insert {
                                    it[userId] = call.principal<JWTPrincipal>()!!.payload.subject.toInt()
                                    it[profilePictureUrl] = publicUrl
                                }
                            }

                    }

                    call.respond(
                        HttpStatusCode.OK, ApiResponse<String>(
                            data = publicUrl,
                            success = true,
                            message = "Image uploaded successfully"
                        )
                    )
                } catch (e: Exception) {
                    application.log.error("Failed to upload product image", e)
                    call.respond(
                        HttpStatusCode.InternalServerError, mapOf(
                            "error" to "Failed to upload image: ${e.message}",
                            "status" to "failed"
                        )
                    )
                }

            }

            post("/user/details") {
                try {
                    // Parse the request body
                    val request = call.receive<UserDetailsRequest>()

                    // Validate user existence
                    transaction {
                        if (Users.selectAll().where { Users.id eq request.userId }.count() == 0L) {
                            throw ApiException(HttpStatusCode.NotFound, "User with ID ${request.userId} does not exist")
                        }
                    }

                    // Update or insert details
                    transaction {
                        val existing =
                            UserDetails.selectAll().where { UserDetails.userId eq request.userId }.firstOrNull()

                        if (existing != null) {
                            UserDetails.update({ UserDetails.userId eq request.userId }) {
                                it[phoneNumber] = request.phoneNumber
                                it[dateOfBirth] = request.dateOfBirth
                                it[profilePictureUrl] = request.profilePictureUrl
                                it[lastLogin] = LocalDateTime.now() // Optional: Update last login
                            }
                        } else {
                            UserDetails.insert {
                                it[userId] = request.userId
                                it[phoneNumber] = request.phoneNumber
                                it[dateOfBirth] = request.dateOfBirth
                                it[profilePictureUrl] = request.profilePictureUrl
                                it[lastLogin] = LocalDateTime.now() // Optional: Set current login time
                            }
                        }
                    }

                    // Respond success
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true, message = "User details updated successfully")
                    )

                } catch (e: ApiException) {
                    call.respondError(e.statusCode, e.message)
                } catch (e: Exception) {
                    call.respondError(
                        HttpStatusCode.InternalServerError,
                        "Failed to update user details",
                        e.message
                    )
                }
            }

            post("/user/address") {
                try {
                    val request = call.receive<UserAddressRequest>()

                    transaction {
                        UserAddress.insert {
                            it[userId] = request.userId
                            it[addressType] = request.addressType
                            it[addressLine1] = request.addressLine1
                            it[addressLine2] = request.addressLine2
                            it[city] = request.city
                            it[state] = request.state
                            it[postalCode] = request.postalCode
                            it[country] = request.country
                        }
                    }

                    call.respond(HttpStatusCode.Created, "User address added successfully")
                } catch (e: Exception) {
                    application.log.error("Error adding user address", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to add user address")
                }
            }

            delete("user/address/{addressId}") {
                try {
                    val addressId = call.parameters["addressId"]?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid or missing address ID")

                    transaction {
                        val deletedCount = UserAddress.deleteWhere { UserAddress.id eq addressId }
                        if (deletedCount == 0) {
                            throw IllegalArgumentException("Address not found")
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<Unit>(success = true, message = "User address deleted successfully")
                    )
                } catch (e: Exception) {
                    application.log.error("Error deleting user address", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete user address")
                }

            }

            post("/user/preferences") {
                try {
                    val request = call.receive<UserPreferencesRequest>()

                    if (transaction {
                            UserPreferences.selectAll().where { UserPreferences.userId eq request.userId }.count() <= 0
                        }) {
                        transaction {
                            UserPreferences.insert {
                                it[userId] = request.userId
                                it[preferredPaymentMethod] = request.preferredPaymentMethod
                                it[shippingSameAsBilling] = request.shippingSameAsBilling
                            }
                        }
                        call.respond(HttpStatusCode.Created, "User preferences added successfully")
                    } else {
                        transaction {
                            UserPreferences.update({ UserPreferences.userId eq request.userId }) {
                                it[preferredPaymentMethod] = request.preferredPaymentMethod
                                it[shippingSameAsBilling] = request.shippingSameAsBilling
                            }
                        }
                        call.respond(HttpStatusCode.Created, "User preferences updated successfully")
                    }


                } catch (e: Exception) {
                    application.log.error("Error adding user preferences", e)
                    call.respond(HttpStatusCode.InternalServerError, "Failed to add user preferences")
                }
            }

            get("/user/details/{userId}") {
                try {
                    // Extract user ID from the request
                    val userId = call.parameters["userId"]?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid or missing user ID")

                    // Retrieve user data in transactions
                    val user = transaction {
                        Users.selectAll().where { Users.id eq userId }
                            .map { it.toUserRes() }
                            .firstOrNull()
                            ?: throw IllegalArgumentException("User not found")
                    }

                    val preferences = transaction {
                        UserPreferences.selectAll().where { UserPreferences.userId eq userId }
                            .map { it.toUserPreferencesRes() }
                            .firstOrNull()
                    }

                    val addresses = transaction {
                        UserAddress.selectAll().where { UserAddress.userId eq userId }
                            .map { it.toUserAddressRes() }
                    }
                    val userDetails = transaction {
                        UserDetails.selectAll().where { UserDetails.userId eq userId }
                            .map { it.toUserAdditionalDetails() }.firstOrNull()
                    }


                    // Build and send response
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = UserDetailsResponse(
                                user = user,
                                additionalDetails = userDetails,
                                preferences = preferences,
                                addresses = addresses,
                            ),
                            message = "User details retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    application.log.error("Error fetching user details", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(
                            success = false,
                            message = "Failed to fetch user details: ${e.message}"
                        )
                    )
                }
            }

            route("/cart"){
                post("/add") {
                    try {
                        val request = call.receive<CartItemRequest>()


                        // Validate request
                        when {
                            request.userId <= 0 -> throw BadRequestException("Invalid user ID")
                            request.productId <= 0 -> throw BadRequestException("Invalid product ID")
                            request.quantity <= 0 -> throw BadRequestException("Quantity must be positive")
                        }

                        val result = transaction {
                            // Find or create cart
                            val cartId = Carts.selectAll().where { Carts.userId eq request.userId }
                                .firstOrNull()?.get(Carts.id)
                                ?: (Carts.insert {
                                    it[userId] = request.userId
                                    it[createdAt] = LocalDateTime.now() // Set created_at
                                    it[updatedAt] = LocalDateTime.now() // Set updated_at
                                    // Rely on database default for created_at and updated_at
                                } get Carts.id)

                            // Fetch product
                            val product = Products.selectAll().where { Products.id eq request.productId }
                                .firstOrNull()
                                ?: throw NotFoundException("Product with ID ${request.productId} not found")
                            val price = product[Products.price]

                            // Check if item exists in cart
                            val existingItem = CartItems.selectAll().where {
                                (CartItems.cartId eq cartId) and (CartItems.productId eq request.productId)
                            }.firstOrNull()

                            if (existingItem != null) {
                                // Update existing item
                                val newQuantity = existingItem[CartItems.quantity] + request.quantity
                                CartItems.update({
                                    (CartItems.cartId eq cartId) and (CartItems.productId eq request.productId)
                                }) {
                                    it[quantity] = newQuantity
                                    it[priceAtAddition] = price * newQuantity.toBigDecimal()
                                    it[updatedAt] = LocalDateTime.now() // Update timestamp
                                    it[CartItems.createdAt] = existingItem[CartItems.createdAt] // Keep original created_at
                                    // Rely on database default for updated_at
                                }
                            } else {
                                // Insert new item
                                CartItems.insert {
                                    it[CartItems.cartId] = cartId
                                    it[CartItems.productId] = request.productId
                                    it[quantity] = request.quantity
                                    it[priceAtAddition] = price * request.quantity.toBigDecimal()
                                    it[CartItems.createdAt] = LocalDateTime.now() // Set created_at
                                    it[CartItems.updatedAt] = LocalDateTime.now() // Set updated_at
                                    // Rely on database default for created_at and updated_at
                                }
                            }

                            cartId
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = mapOf("cartId" to result),
                                error = null
                            )
                        )
                        //logger.info("Successfully added item to cart for user ${request.userId}, product ${request.productId}")
                    } catch (e: BadRequestException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, error =ApiError(
                                details = e.message,
                                code =1,
                            ), message = "",
                            )
                        )
                       // logger.warn("Bad request: ${e.message}")
                    } catch (e: NotFoundException) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, message = e.message)
                        )
                        //logger.warn("Not found: ${e.message}")
                    } catch (e: Exception) {
                       // logger.error("Error adding item to cart", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, message = "Failed to add item to cart: ${e.message}")
                        )
                    }
                }

                get("/{userId}") {
                    try {
                        val userId = call.parameters["userId"]?.toIntOrNull()
                            ?: throw BadRequestException("Invalid User ID format")

                        if (userId <= 0) {
                            throw BadRequestException("Invalid user ID")
                        }

                        val cartResponse = transaction {
                            val cart = Carts.selectAll().where { Carts.userId eq userId }
                                .firstOrNull()
                                ?: throw NotFoundException("Cart not found for user ID $userId")

                            val cartId = cart[Carts.id]
                            val createdAt = cart[Carts.createdAt].toString() // Convert DateTime to String
                            val updatedAt = cart[Carts.updatedAt].toString() // Convert DateTime to String

                            val cartItems = (CartItems innerJoin Products)
                                .selectAll()
                                .where { CartItems.cartId eq cartId }
                                .map {
                                    CartItemResponse(
                                        cartItemId = it[CartItems.id],
                                        productId = it[CartItems.productId],
                                        productName = it[Products.name],
                                        quantity = it[CartItems.quantity],
                                        priceAtAddition = it[CartItems.priceAtAddition],
                                        imageUrl = it[Products.imageUrl]
                                    )
                                }

                            CartResponse(
                                cartId = cartId,
                                userId = userId,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                items = cartItems
                            )
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = cartResponse,
                                error = null
                            )
                        )
                        //logger.info("Successfully retrieved cart for user $userId")
                    } catch (e: BadRequestException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, error = ApiError(
                                details = e.message,
                                code = 1,
                            ), message = "",
                            )
                        )
                        // logger.warn("Bad request: ${e.message}")
                    } catch (e: NotFoundException) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, message = e.message)
                        )
                        //logger.warn("Not found: ${e.message}")
                    } catch (e: Exception) {
                        // logger.error("Error getting cart", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, message = "Failed to retrieve cart: ${e.message}")
                        )
                    }
                }


            }

            route("/product"){
                get("/products") {
                    try {
                        val products = transaction {
                            Products.selectAll().map { row ->
                                ProductResponse(
                                    id = row[Products.id],
                                    name = row[Products.name],
                                    description = row[Products.description],
                                    price = row[Products.price].toDouble(), // Convert BigDecimal from DB to Double
                                    imageUrl = row[Products.imageUrl],
                                    categoryId = row[Products.categoryId],
                                    subcategoryId = row[Products.subcategoryId],
                                    type = row[Products.type],
                                    moreDetails = row[Products.moreDetails],
                                    createdAt = row[Products.createdAt],
                                    updatedAt = row[Products.updatedAt]
                                )
                            }
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = products,
                                error = null
                            )
                        )
                        //logger.info("Successfully retrieved all products")
                    } catch (e: Exception) {
                        //logger.error("Error retrieving products", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(success = false, message = "Failed to retrieve products: ${e.message}")
                        )
                    }
                }
            }

        }


    }
}

private fun sanitizeFilename(filename: String): String {
    return filename.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
}

private fun isValidImage(contentType: ContentType?): Boolean {
    val allowedTypes = setOf(
        ContentType.Image.JPEG,
        ContentType.Image.PNG,
        ContentType.Image.Any

    )

    // Allow `application/octet-stream` as a fallback
    if (contentType == ContentType.Application.OctetStream) {
        return true // Optional: Validate file extension or other criteria
    }
    return contentType in allowedTypes
}

// Helper extension for error responses
suspend fun ApplicationCall.respondError(
    statusCode: HttpStatusCode,
    message: String,
    details: String? = null
) {
    respond(
        statusCode,
        ApiResponse<Unit>(
            success = false,
            error = ApiError(
                code = statusCode.value,
                details = details
            ),
            message = message
        )
    )
}

