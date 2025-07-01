package models.responses
import kotlinx.serialization.Serializable

@Serializable
data class CartItemRequest(
    val userId: Int,
    val productId: Int,
    val quantity: Int
)