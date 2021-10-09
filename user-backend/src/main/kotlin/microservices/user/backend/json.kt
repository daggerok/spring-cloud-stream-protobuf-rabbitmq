package microservices.user.backend

import java.time.LocalDateTime
import microservices.user.api.converters.toLocalDateTime

data class User(
    val id: String = "",
    val username: String = "",
    val createdAt: LocalDateTime = 0.toLocalDateTime(),
)
