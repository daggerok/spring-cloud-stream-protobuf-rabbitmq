package microservices.user.backend

import java.util.UUID
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus

@TestInstance(PER_CLASS)
@SpringBootTest(
    properties = [
        "user.port=8002",
        "user.host=127.0.0.1",
    ],
    webEnvironment = RANDOM_PORT,
)
@DisplayName("UserBackend e2e tests")
@DisplayNameGeneration(ReplaceUnderscores::class)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@EnableAutoConfiguration(exclude = [TestSupportBinderAutoConfiguration::class]) // disable test binder
class UserClientIT @Autowired constructor(
    val restTemplate: TestRestTemplate,
    @LocalServerPort port: Int,
) {

    val baseUrl = "http://127.0.0.1:$port"

    @Test
    fun should_create_user_get_user_and_get_user_by_id() {
        // given
        val firstResponse = restTemplate.exchange<List<User>>("$baseUrl/api/v1/users", GET)
        log.info { "firstResponse: $firstResponse" }

        // and
        val before = firstResponse.body ?: fail("1...")
        log.info { "before: ${before.toList().size}" }

        // when
        val requestEntity = HttpEntity(User(username = "create@me.com ${UUID.randomUUID()}"))
        val secondResponse = restTemplate.exchange<Unit>("$baseUrl/api/v1/users", POST, requestEntity)
        log.info { "secondResponse: $secondResponse" }

        // and
        assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        runCatching { Thread.sleep(2345) }

        // then
        val thirdResponse = restTemplate.exchange<List<User>>("$baseUrl/api/v1/users", GET)
        log.info { "thirdResponse: $thirdResponse" }

        // and
        val after = thirdResponse.body ?: fail("2...")
        log.info { "after: ${after.size}" }
        assertThat(after.size).isEqualTo(before.toList().size + 1)

        // given
        val givenId = after.first().id

        // when
        val fourthResponse = restTemplate.exchange<User>("$baseUrl/api/v1/users/{id}", GET, null, givenId)
        val user = fourthResponse.body ?: fail("3.")
        assertThat(user.id).isEqualTo(givenId)
        assertThat(user.username).startsWith("create@me")
    }

    companion object {
        val log = logger()
    }
}
