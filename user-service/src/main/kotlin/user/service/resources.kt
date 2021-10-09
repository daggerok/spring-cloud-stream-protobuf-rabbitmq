package user.service

import java.util.UUID
import microservices.user.api.converters.toTimestampOrDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import user.api.UserApiProtos.UserDTO
import user.api.UserApiProtos.UserDocument
import user.api.UserApiProtos.UsersDocument

@RestController
class UserResource(private val userRepository: UserRepository) {

    @GetMapping("/api/v1/users/{id}")
    fun getUser(@PathVariable("id") id: String): UserDocument =
        userRepository.findById(UUID.fromString(id))
            .map {
                UserDocument.newBuilder()
                    .setUserDTO(
                        UserDTO.newBuilder()
                            .setId(it.id.toString())
                            .setUsername(it.username)
                            .setCreatedAt(it.createdAt.toTimestampOrDefault())
                    )
                    .build()
            }
            .orElseThrow { NoSuchUserException(id) }

    @GetMapping("/api/v1/users")
    fun getUsers(): UsersDocument =
        userRepository.findAll().let {
            // if (it.isEmpty()) return@let UsersDocument.getDefaultInstance()
            UsersDocument.newBuilder().run {
                for (user in it) {
                    addUserDTOs(
                        UserDTO.newBuilder()
                            .setId(user.id.toString())
                            .setUsername(user.username)
                            .setCreatedAt(user.createdAt.toTimestampOrDefault())
                    )
                }
                build()
            }
        }
}
