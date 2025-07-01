package models.responses

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

@Serializable
data class CartItemResponse(
    val cartItemId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val priceAtAddition: BigDecimal,
    val imageUrl: String? = null
)

@Serializable
data class CartResponse(
    val cartId: Int,
    val userId: Int,
    val createdAt: String, // Or DateTime if you prefer
    val updatedAt: String, // Or DateTime if you prefer
    val items: List<CartItemResponse>
)

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString()) // Convert BigDecimal to a plain string
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString()) // Convert string back to BigDecimal
    }
}