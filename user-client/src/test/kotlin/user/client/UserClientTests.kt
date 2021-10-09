package user.client

import com.github.tomakehurst.wiremock.client.WireMock
import java.util.UUID
import org.apache.logging.log4j.kotlin.logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import user.api.UserApiProtos.UserDTO
import user.api.UserApiProtos.UserDocument
import user.api.UserApiProtos.UsersDocument

@SpringBootApplication
internal class UserClientTestsApp

@TestInstance(PER_CLASS)
@AutoConfigureWireMock(port = 0)
@DisplayName("User feign client tests")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DisplayNameGeneration(ReplaceUnderscores::class)
@ContextConfiguration(classes = [UserClientTestsApp::class])
class UserClientTests(@Autowired val userClient: UserClient) {

    @Test
    fun should_get_users() {
        // setup
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

        // given
        val document = userClient.getUsers()
        log.info { "document: $document" }

        // then
        val dtos = document.userDTOsList
        log.info { "dtos: $dtos" }
        assertThat(dtos).hasSize(2)

        // and
        val first = dtos.first()
        log.info { "dto: $first" }
        assertThat(first.username).isEqualTo("first@gmail.com")

        // and
        val second = dtos.last()
        log.info { "second: $second" }
        assertThat(second.username).isEqualTo("second@gmail.com")
    }

    @Test
    fun should_get_user_by_id() {
        // setup
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

        // given
        val document = userClient.getUserById(givenId)
        log.info { "document: $document" }

        // then
        val dto = document.userDTO
        log.info { "dto: $dto" }
        assertThat(dto.username).isEqualTo("daggerok@gmail.com")
    }

    companion object {
        val log = logger()
    }
}
