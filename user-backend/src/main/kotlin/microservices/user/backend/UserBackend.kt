package microservices.user.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UserBackend

fun main(args: Array<String>) {
    runApplication<UserBackend>(*args)
}
