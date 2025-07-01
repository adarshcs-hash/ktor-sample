import kotlinx.serialization.Contextual
import models.UserDetails
import models.Users
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import models.UserAddress
import models.UserPreferences

@Serializable
data class UserProfileResponse(
    val user: UserResponse,
    val profile: UserDetailsResponse?,
    val address: UserAddressResponse?
)

@Serializable
data class UserDetailsResponse(
    val user: UserResponse,
    val  additionalDetails: UserAdditionalDetailsResponse?,
    val preferences: UserPreferencesResponse?,
    val addresses: List<UserAddressResponse>,

)
@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    val name: String?,
)

@Serializable
data class UserAddressResponse(
    val id: Int? = null, // Optional ID for existing addresses
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
)

@Serializable
data class UserPreferencesResponse(
    val preferredPaymentMethod: String?,
    val shippingSameAsBilling: Boolean
)

@Serializable
data class UserAdditionalDetailsResponse(
    val phoneNumber: String?,
    val dob: String?,
    val profileImageUrl: String?,
)

// Extension functions



fun ResultRow.toUserRes() = UserResponse(
    id = this[Users.id],
    email = this[Users.email],
    name = this[Users.name],
)

fun ResultRow.toUserPreferencesRes() = UserPreferencesResponse(
    preferredPaymentMethod = this[UserPreferences.preferredPaymentMethod],
    shippingSameAsBilling = this[UserPreferences.shippingSameAsBilling]
)

fun ResultRow.toUserAddressRes() = UserAddressResponse(
    id = this[UserAddress.id],
    addressLine1 = this[UserAddress.addressLine1],
    addressLine2 = this[UserAddress.addressLine2],
    city = this[UserAddress.city],
    state = this[UserAddress.state],
    postalCode = this[UserAddress.postalCode],
    country = this[UserAddress.country]
)

fun ResultRow.toUserAdditionalDetails() = UserAdditionalDetailsResponse(
    phoneNumber = this[UserDetails.phoneNumber] ,
    dob = this[UserDetails.dateOfBirth]?.let { LocalDate.parse(it) }?.toString(),
    profileImageUrl = this[UserDetails.profilePictureUrl]?.takeIf { it.isNotBlank() } ?: ""
)

