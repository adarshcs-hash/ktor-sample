import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

object TokenManager {
    private val jwtSecret = System.getenv("JWT_SECRET")!!
    private val jwtIssuer = System.getenv("JWT_ISSUER")!!
    val accessTokenExpiry = 3_600_000L // 1 hour
    private val refreshTokenExpiry = 30L * 24 * 3_600_000L // 30 days

    fun generateAccessToken(userId: Int, email: String): String {
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withIssuer(jwtIssuer)
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiry))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    fun generateRefreshToken(): String = UUID.randomUUID().toString()
}