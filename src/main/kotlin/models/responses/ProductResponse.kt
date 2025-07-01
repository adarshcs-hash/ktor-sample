package models.responses

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double, // Using Double as per last user request to avoid BigDecimal issues.
    // NOTE: For financial data, BigDecimal is generally preferred for precision.
    val imageUrl: String?,
    val categoryId: Int,
    val subcategoryId: Int?,
    val type: String?,
    val moreDetails: String?, // Mapped to String as per ORM text type
    val createdAt: String,
    val updatedAt: String
)