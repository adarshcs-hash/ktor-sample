import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption

object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100).uniqueIndex()
    val createdAt = varchar("created_at", 50).default("CURRENT_TIMESTAMP")
    val updatedAt = varchar("updated_at", 50).default("CURRENT_TIMESTAMP")
    override val primaryKey = PrimaryKey(id)
}

object Subcategories : Table("subcategories") {
    val id = integer("id").autoIncrement()
    val categoryId = integer("category_id").references(Categories.id, onDelete = ReferenceOption.RESTRICT)
    val name = varchar("name", 100)
    val createdAt = varchar("created_at", 50).default("CURRENT_TIMESTAMP")
    val updatedAt = varchar("updated_at", 50).default("CURRENT_TIMESTAMP")
    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex("unique_category_subcategory", categoryId, name)
    }
}

object Products : Table("products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val price = decimal("price", 10, 2).check { it.greaterEq(0) }
    val imageUrl = text("image_url").nullable()
    val categoryId = integer("category_id").references(Categories.id, onDelete = ReferenceOption.RESTRICT)
    val subcategoryId = integer("subcategory_id").references(Subcategories.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val type = varchar("type", 100).nullable()
    val moreDetails = text("more_details").nullable() // Changed from jsonb to text
    val createdAt = varchar("created_at", 50).default("CURRENT_TIMESTAMP")
    val updatedAt = varchar("updated_at", 50).default("CURRENT_TIMESTAMP")
    override val primaryKey = PrimaryKey(id)
    init {
        index(false, categoryId)
        index(false, subcategoryId)
        index(false, name)
    }
}