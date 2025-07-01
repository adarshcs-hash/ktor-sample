import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val message: String? = null
)

@Serializable
data class ApiError(
    val code: Int,
    val details: String? = null,
    val validationErrors: Map<String, String>? = null
)