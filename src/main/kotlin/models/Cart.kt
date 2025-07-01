package models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime


object Carts : Table("carts") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
    init {
        index(false, userId)
    }
}

object CartItems : Table("cart_items") {
    val id = integer("id").autoIncrement()
    val cartId = integer("cart_id").references(Carts.id, onDelete = ReferenceOption.CASCADE)
    val productId = integer("product_id").references(Products.id, onDelete = ReferenceOption.RESTRICT)
    val quantity = integer("quantity").check { it.greater(0) }
    val priceAtAddition = decimal("price_at_addition", 10, 2)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex("unique_cart_product", cartId, productId)
        index(false, cartId)
    }
}