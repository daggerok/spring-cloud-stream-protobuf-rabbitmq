package com.github.daggerok.spring_cloud_stream_rabbitmq_protobuf

import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(PER_CLASS)
@DisplayNameGeneration(ReplaceUnderscores::class)
class ProducerApplicationTests {

    @Test
    fun main_test() { }
}
