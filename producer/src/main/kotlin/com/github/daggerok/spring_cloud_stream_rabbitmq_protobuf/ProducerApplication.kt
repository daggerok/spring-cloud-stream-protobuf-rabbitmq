package com.github.daggerok.spring_cloud_stream_rabbitmq_protobuf

import com.github.daggerok.protorabbit.ApiProtos.Greeting
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.util.MimeType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ProducerResource(private val streamBridge: StreamBridge) {

    @PostMapping
    fun produce(@RequestBody map: Map<String, String>) {
        val message = map.getOrDefault("message", "undefined message")
        val greeting = Greeting.newBuilder()
            .setMessage(message)
            .build()
        val sent = streamBridge.send(
            "producingFunction-out-0", greeting,
            MimeType.valueOf("application/x-protobuf")
        )
        log.info { "$greeting has been sent: $sent." }
    }

    companion object {
        private val log = logger()
    }
}

@SpringBootApplication
class ProducerApplication

fun main(args: Array<String>) {
    runApplication<ProducerApplication>(*args)
}
