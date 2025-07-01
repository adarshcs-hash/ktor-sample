package models.responses

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class UserDetailsRequest(
    val userId: Int, // Reference to the existing user ID
    val phoneNumber: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    @Contextual
    val dateOfBirth: String? = null,
    val profilePictureUrl: String? = null,
    val preferredPaymentMethod: String? = null,
    val shippingSameAsBilling: Boolean = true
)
