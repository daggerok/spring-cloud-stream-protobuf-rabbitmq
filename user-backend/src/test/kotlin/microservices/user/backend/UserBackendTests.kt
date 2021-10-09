package microservices.user.backend

import com.github.tomakehurst.wiremock.client.WireMock
import java.util.UUID
import microservices.protobuf.spring.cloud.stream.ProtobufSpringCloudStreamAutoConfiguration.Companion.protobufMimeType
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.messaging.MessageChannel
import user.api.UserApiProtos.CreateUserEvent
import user.api.UserApiProtos.UserDTO
import user.api.UserApiProtos.UserDocument
import user.api.UserApiProtos.UsersDocument

@TestInstance(PER_CLASS)
@AutoConfigureWireMock(port = 0)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayNameGeneration(ReplaceUnderscores::class)
@DisplayName("UserBackend feign client WireMock tests")
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
// @EnableAutoConfiguration(exclude = [MessageCollectorAutoConfiguration::class]) // disable test binder
class UserClientTests @Autowired constructor(
    @Autowired @Qualifier("createUserEventStream-out-0") val output: MessageChannel,
    val messageCollector: MessageCollector,
    val restTemplate: TestRestTemplate,
    @LocalServerPort port: Int,
) {

    val baseUrl = "http://127.0.0.1:$port"

    @Test
    fun should_get_users() {
        // given
        WireMock.stubFor(
            WireMock.get("/api/v1/users")
                .withHeader("Content-Type", WireMock.containing("application/x-protobuf"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/x-protobuf")
                        .withBody(
                            UsersDocument.newBuilder()
                                .addUserDTOs(
                                    UserDTO.newBuilder()
                                        .setId(UUID.randomUUID().toString())
                                        .setUsername("first@gmail.com")
                                )
                                .addUserDTOs(
                                    UserDTO.newBuilder()
                                        .setId(UUID.randomUUID().toString())
                                        .setUsername("second@gmail.com")
                                )
                                .build()
                                .toByteArray()
                        )
                )
        )

        // when
        val response = restTemplate.exchange<List<User>>("$baseUrl/api/v1/users", GET)
        log.info { "response: $response" }

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // and
        val iterable = response.body ?: fail("body may not be null")
        log.info { "iterable: $iterable" }
        assertThat(iterable).hasSize(2)
        assertThat(iterable.first().username).isEqualTo("first@gmail.com")
        assertThat(iterable.last().username).isEqualTo("second@gmail.com")
    }

    @Test
    fun should_get_user_by_id() {
        // given
        val givenId = UUID.randomUUID().toString()

        // and
        WireMock.stubFor(
            WireMock.get("/api/v1/users/$givenId")
                .withHeader("Content-Type", WireMock.containing("application/x-protobuf"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/x-protobuf")
                        .withBody(
                            UserDocument.newBuilder()
                                .setUserDTO(
                                    UserDTO.newBuilder()
                                        .setId(givenId)
                                        .setUsername("daggerok@gmail.com")
                                )
                                .build()
                                .toByteArray()
                        )
                )
        )

        // when
        val response = restTemplate.exchange<User>("$baseUrl/api/v1/users/{id}", GET, null, givenId)
        log.info { "response: $response" }

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // and
        val user = response.body ?: fail("body may not be null")
        log.info { "user: $user" }
        assertThat(user.id).isEqualTo(givenId)
        assertThat(user.username).isEqualTo("daggerok@gmail.com")
    }

    @Test
    fun should_create_user() {
        // given
        val requestEntity = HttpEntity(
            User(
                username = "create@me.com"
            )
        )
        log.info { "requestEntity: $requestEntity" }

        // when
        val response = restTemplate.exchange<Unit>("$baseUrl/api/v1/users", POST, requestEntity)
        log.info { "requestEntity: $response" }

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        // and
        val blockingQueue = messageCollector.forChannel(output)
        log.info { "blockingQueue: $blockingQueue" }
        assertThat(blockingQueue).hasSize(1)

        // and
        val message = blockingQueue.peek()
        log.info { "message: $message" }
        assertThat(message.headers["contentType"]).isEqualTo(protobufMimeType)
        assertThat(message.headers["protobufClassName"]).isEqualTo(CreateUserEvent::class.java.name)
        assertThat(message.payload).isEqualTo(
            CreateUserEvent.newBuilder()
                .setUserDTO(
                    UserDTO.newBuilder()
                        .setUsername("create@me.com")
                )
                .build()
                .toByteArray()
        )
    }

    companion object {
        val log = logger()
    }
}
