package extensions

import UserResponse
import models.User
import models.Users
import org.jetbrains.exposed.sql.ResultRow

// Converts a ResultRow from Users table to a User object
fun ResultRow.toUser(): User {
    return User(
        id = this[Users.id],
        email = this[Users.email],
        passwordHash = this[Users.passwordHash],
        name = this[Users.name]  // Handles nullable field automatically
    )
}

