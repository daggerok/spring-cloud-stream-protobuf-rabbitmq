package com.github.daggerok.spring.rabbit.amqp.protobuf

import java.lang.reflect.Method
import org.apache.logging.log4j.kotlin.logger
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType

@Configuration
@ConditionalOnMissingClass
class SpringRabbitAmqpProtobufAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun protobufAmqpMessageConverter(): org.springframework.amqp.support.converter.AbstractMessageConverter {
        return object : org.springframework.amqp.support.converter.AbstractMessageConverter() {
            override fun fromMessage(message: org.springframework.amqp.core.Message): Any {
                if (!com.google.protobuf.Message::class.java.isAssignableFrom(message.javaClass))
                    throw MessageConversionException("Message is not protobuf: $message")
                val contentType = message.messageProperties.contentType
                val isProtobuf = contentType.contains(protobufContentType)
                if (!isProtobuf) throw MessageConversionException("Message content-type is not protobuf: $contentType")
                val result = runCatching {
                    val protobufClassName = message.messageProperties.headers[protobufClassName] as String
                    val protobufClass = Class.forName(protobufClassName)
                    val builder = protobufBuilderOf(protobufClass)
                    builder.mergeFrom(message.body).buildPartial()
                        .apply { builder.clear() }
                }
                if (result.isFailure) throw IllegalArgumentException(
                    "Could not initiate Message.Builder from $protobufClassName",
                    result.exceptionOrNull(),
                )
                return result.getOrThrow()
                    .apply { log.info { "Convert AMQP message into protobuf: $this" } }
            }

            override fun createMessage(obj: Any, props: MessageProperties): org.springframework.amqp.core.Message {
                if (!com.google.protobuf.Message::class.java.isAssignableFrom(obj.javaClass))
                    throw MessageConversionException("Object is not a protobuf: $obj")
                val protobuf = obj as com.google.protobuf.Message
                val byteArray = protobuf.toByteArray()
                props.headers[protobufClassName] = obj::class.java.name
                props.contentType = protobufContentType
                props.contentLength = byteArray.size as Long
                return org.springframework.amqp.core.Message(byteArray, props)
                    .apply { log.info { "Convert protobuf into AMQP message: $this" } }
            }
        }
    }

    companion object {
        const val protobufClassName = "protobuf_class_name"
        const val protobufContentType = "application/x-protobuf"
        val protobufMimeType = MimeType.valueOf(protobufContentType) // unused

        private val log = logger()
        private val cache = ConcurrentReferenceHashMap<Class<*>, Method>()

        private fun protobufBuilderOf(type: Class<*>): com.google.protobuf.Message.Builder = run {
            val aTry = kotlin.runCatching {
                val method = cache[type] ?: type.getMethod("newBuilder")
                cache.putIfAbsent(type, method)
                method.invoke(type) as com.google.protobuf.Message.Builder
            }
            if (aTry.isFailure)
                throw IllegalArgumentException(
                    "Cannot initiate com.google.protobuf.Message.Builder from $protobufClassName",
                    aTry.exceptionOrNull()
                )
            aTry.getOrNull() ?: throw IllegalArgumentException("A com.google.protobuf.Message.Builder may not be null")
        }
    }
}
