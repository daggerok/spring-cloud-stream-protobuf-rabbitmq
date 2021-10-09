package user.service

import java.util.function.Consumer
import org.apache.logging.log4j.kotlin.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import user.api.UserApiProtos.CreateUserEvent

@Configuration
class CreateUserEventHandlerConfig(private val userRepository: UserRepository) {

    @Bean
    fun createUserEventStream() = Consumer<CreateUserEvent> {
        log.info { "Consuming: $it" }
        val user = User(username = it.userDTO.username)
        userRepository.save(user)
    }

    companion object {
        val log = logger()
    }
}
