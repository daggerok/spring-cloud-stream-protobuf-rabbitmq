package user.service

import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT
import org.springframework.context.annotation.Import
import user.client.UserClient
import user.client.autoconfigure.UserClientAutoConfiguration

@TestInstance(PER_CLASS)
@DisplayName("UserResource tests")
@SpringBootTest(
    webEnvironment = DEFINED_PORT,
    properties = [
        "server.port=8765",
        "user.host=127.0.0.1",
        "user.port=\${server.port}",
    ],
)
@Import(UserClientAutoConfiguration::class)
@DisplayNameGeneration(ReplaceUnderscores::class)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class UserClientTests @Autowired constructor(val userRepository: UserRepository, val userClient: UserClient) {

    @BeforeEach
    internal fun setUp() {
        // setup
        userRepository.findAll().run {
            if (this.iterator().hasNext()) {
                log.info { "Found user data. Cleaning before test..." }
                userRepository.deleteAllInBatch()
            }
        }
    }

    @Test
    fun should_get_users() {
        // given
        val givenUser = User(username = "daggerok@gmail.com")

        // and
        log.info { "save givenUser: $givenUser" }
        userRepository.save(givenUser)

        // then
        val document = userClient.getUsers()
        log.info { "document: $document" }

        // and
        val dtos = document.userDTOsList
        log.info { "dtos: $dtos" }
        assertThat(dtos).hasSize(1)

        // and
        val dto = dtos.first()
        log.info { "dto: $dto" }
        assertThat(dto.username).isEqualTo("daggerok@gmail.com")
    }

    @Test
    fun should_get_user_by_id() {
        // given
        val maybeUser = userRepository.save(
            User(
                username = "daggerok@gmail.com",
            )
        )

        // and
        val givenUserId = maybeUser.get().id

        // then
        val document = userClient.getUserById(givenUserId.toString())
        log.info { "document: $document" }

        // and
        val dto = document.userDTO
        log.info { "dto: $dto" }
        assertThat(dto.username).isEqualTo("daggerok@gmail.com")
    }

    companion object {
        val log = logger()
    }
}
