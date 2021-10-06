package com.github.daggerok.spring.cloud.stream.rabbit.protobuf

import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@SpringBootApplication
@TestInstance(PER_CLASS)
@DisplayNameGeneration(ReplaceUnderscores::class)
class Tests {

    @Test
    fun test_context() { }
}
