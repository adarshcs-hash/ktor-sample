import io.ktor.http.HttpStatusCode

// Custom Exceptions.kt
open class ApiException(
    val statusCode: HttpStatusCode,
    override val message: String,
    val errorCode: Int = statusCode.value,
    val details: String? = null
) : Exception(message)

class AuthenticationException(message: String, details: String? = null) :
    ApiException(HttpStatusCode.Unauthorized, message, 401, details)

class AuthorizationException(message: String, details: String? = null) :
    ApiException(HttpStatusCode.Forbidden, message, 403, details)

class NotFoundException(message: String, details: String? = null) :
    ApiException(HttpStatusCode.NotFound, message, 404, details)

class ValidationException(
    val errors: Map<String, String>,
    details: String? = null
) : ApiException(HttpStatusCode.BadRequest, "Validation failed", 400, details) {
    constructor(field: String, error: String) : this(mapOf(field to error))
}