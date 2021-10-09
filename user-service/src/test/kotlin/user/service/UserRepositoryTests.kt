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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@TestInstance(PER_CLASS)
@DisplayName("UserRepository tests")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayNameGeneration(ReplaceUnderscores::class)
class UserRepositoryTests @Autowired constructor(val userRepository: UserRepository) {

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
    fun user_repository_should_work() {
        // given
        val givenUser = User(username = "daggerok@gmail.com")

        // when
        log.info { "save givenUser: $givenUser" }
        userRepository.save(givenUser)

        // then
        val allUsers = userRepository.findAll()
        log.info { "allUsers: $allUsers" }
        assertThat(allUsers).hasSize(1)

        // and
        val maybeUser = userRepository.findById(allUsers.first().id)
        log.info { "maybeUser: $maybeUser" }
        assertThat(maybeUser).isPresent

        val aUser = maybeUser.get()
        log.info { "aUser: $aUser" }
        assertThat(aUser.username).isEqualTo("daggerok@gmail.com")
    }

    companion object {
        val log = logger()
    }
}
