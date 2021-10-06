package com.github.daggerok.spring.cloud.stream.rabbit.protobuf

import java.lang.reflect.Method
import java.util.Optional
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.support.MessageBuilder
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType

@Configuration
@ConditionalOnMissingClass
class SpringCloudStreamRabbitProtobufAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun protobufSpringCloudMessageConverter(): MessageConverter = object : MessageConverter {
        override fun fromMessage(message: Message<*>, targetClass: Class<*>): Any? =
            delegate.fromMessage(message, targetClass)
                ?.apply { log.info { "Convert spring cloud message into protobuf: $this" } }

        override fun toMessage(payload: Any, headers: MessageHeaders?): Message<*>? =
            Optional
                .ofNullable(delegate.toMessage(payload, headers))
                .map {
                    MessageBuilder
                        .fromMessage(it)
                        .setHeader("content_type", protobufContentType)
                        .setHeader("amqp_contentType", protobufContentType)
                        .setHeader(protobufClassName, payload::class.java.name)
                        .build()
                }
                .orElse(null)
                ?.apply { log.info { "Convert protobuf into spring cloud message: $this" } }
    }

    companion object {
        const val protobufClassName = "protobuf_class_name"
        const val protobufContentType = "application/x-protobuf"
        val protobufMimeType = MimeType.valueOf(protobufContentType)

        private val log = logger()
        private val cache = ConcurrentReferenceHashMap<Class<*>, Method>()

        private val delegate =
            object : AbstractMessageConverter(protobufMimeType) {
                override fun supports(type: Class<*>): Boolean =
                    com.google.protobuf.Message::class.java.isAssignableFrom(type)
                        .apply { log.info { "Delegate supports $type: $this" } }

                override fun convertFromInternal(message: Message<*>, type: Class<*>, unused: Any?): Any? {
                    val aTry = runCatching {
                        val builder = protobufBuilderOf(type)
                        builder.mergeFrom(message.payload as ByteArray).buildPartial()
                            .apply { builder.clear() }
                    }
                    if (aTry.isFailure) throw IllegalArgumentException(aTry.exceptionOrNull())
                    return aTry.getOrNull()
                        ?.apply { log.info { "Delegate converter from internal $this" } }
                }

                override fun convertToInternal(payload: Any, headers: MessageHeaders?, unused: Any?): Any? =
                    com.google.protobuf.Message::class.java.cast(payload).toByteArray()
                        ?.apply { log.info { "Delegate convert into internal $this" } }
            }

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
