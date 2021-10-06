package com.github.daggerok.spring_cloud_stream_rabbitmq_protobuf

import com.github.daggerok.protorabbit.ApiProtos.Greeting
import java.util.function.Consumer
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ConsumerConfig {

    @Bean
    fun consumingFunction() = Consumer<Greeting> {
        log.info { "Consuming: $it" }
    }

    companion object {
        val log = logger()
    }
}

@SpringBootApplication
class ConsumerApplication

fun main(args: Array<String>) {
    runApplication<ConsumerApplication>(*args)
}
