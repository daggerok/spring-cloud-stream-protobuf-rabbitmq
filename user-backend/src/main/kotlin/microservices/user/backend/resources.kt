package microservices.user.backend

import java.util.Optional
import microservices.user.api.converters.toLocalDateTime
import org.springframework.http.HttpStatus.CREATED
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import user.api.UserApiProtos.CreateUserEvent
import user.api.UserApiProtos.UserDTO
import user.client.UserClient

@RestController
class UserResource(private val userClient: UserClient, private val evenStream: EvenStream) {

    @ResponseStatus(CREATED)
    @PostMapping("/api/v1/users")
    fun createUser(@RequestBody user: User) =
        evenStream.send(
            CreateUserEvent.newBuilder()
                .setUserDTO(
                    UserDTO.newBuilder()
                        .setUsername(user.username)
                )
                .build()
        )

    @GetMapping("/api/v1/users/{id}")
    fun getUser(@PathVariable("id") id: String): User =
        userClient.getUserById(id).run {
            User(
                id = userDTO.id,
                username = userDTO.username,
                createdAt = userDTO.createdAt.toLocalDateTime(),
            )
        }

    @GetMapping("/api/v1/users")
    fun getUsers(): List<User> =
        Optional.ofNullable(userClient.getUsers())
            .map {
                it.userDTOsList.map {
                    User(
                        id = it.id,
                        username = it.username,
                        createdAt = it.createdAt.toLocalDateTime(),
                    )
                }
            }
            .orElse(listOf())
}
