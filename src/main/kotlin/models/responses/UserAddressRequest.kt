package models.responses
import kotlinx.serialization.Serializable

@Serializable
data class UserAddressRequest(
    val userId: Int,
    val addressLine1: String,
    val addressType: String,
    val addressLine2: String? = null,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)
