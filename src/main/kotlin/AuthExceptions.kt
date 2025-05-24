import io.ktor.http.HttpStatusCode

class AuthenticationException(
    message: String = "Invalid credentials",
    val statusCode: HttpStatusCode = HttpStatusCode.Unauthorized
) : RuntimeException(message)