package user.service

import java.util.UUID

data class InvalidUserException(val id: UUID) : RuntimeException("Invalid user identity: $id")

data class NoSuchUserException(val id: String) : RuntimeException("User($id) not found")
