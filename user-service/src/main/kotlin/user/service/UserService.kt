package user.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UserService

fun main(args: Array<String>) {
    runApplication<UserService>(*args)
}
