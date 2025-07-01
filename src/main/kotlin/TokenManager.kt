import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

object TokenManager {
    const val ACCESS_TOKEN_EXPIRATION_SECONDS = 3600
    const val TOKEN_TYPE = "Bearer"
    private val jwtSecret = System.getenv("JWT_SECRET")!!
    private val jwtIssuer = System.getenv("JWT_ISSUER")!!
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: throw IllegalStateException("JWT_AUDIENCE not found")
    val accessTokenExpiry = 6000_000;// 1 hour

    //val accessTokenExpiry = 60_000 // 1 minute in milliseconds

    private val refreshTokenExpiry = 30L * 24 * 3_600_000L // 30 days

    fun generateAccessToken(userId: Int, email: String): String {
        println("Generating token with audience: $jwtAudience, issuer: $jwtIssuer")
        val token = JWT.create()
            .withSubject(userId.toString())
            .withAudience(jwtAudience)
            .withClaim("email", email)
            .withIssuer(jwtIssuer)
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiry))
            .sign(Algorithm.HMAC256(jwtSecret))
        println("Generated token: $token")
        debugToken(token) // Debug the token
        return token
    }

    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    fun debugToken(token: String) {
        val decodedJWT = JWT.decode(token)
        println("Audience: ${decodedJWT.audience}")
        println("Issuer: ${decodedJWT.issuer}")
        println("Claims: ${decodedJWT.claims}")
    }
}