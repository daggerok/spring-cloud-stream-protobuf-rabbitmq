package microservices.protobuf.spring.cloud.stream

import java.lang.reflect.Method
import java.util.Optional
import org.apache.logging.log4j.kotlin.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.support.MessageBuilder
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType

@Configuration
@ConditionalOnClass(com.google.protobuf.Message::class)
class ProtobufSpringCloudStreamAutoConfiguration {

    @Bean
    @Primary
    // @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(MessageConverter::class)
    fun protobufSpringCloudMessageConverter(): MessageConverter = object : MessageConverter {
        override fun fromMessage(message: Message<*>, targetClass: Class<*>): Any? =
            delegate.fromMessage(message, targetClass)
                ?.apply { log.info { "Convert spring cloud message => protobuf: $this" } }

        override fun toMessage(payload: Any, headers: MessageHeaders?): Message<*>? =
            Optional
                .ofNullable(delegate.toMessage(payload, headers))
                .map {
                    MessageBuilder
                        .fromMessage(it)
                        .setHeader(protobufContentTypeHeaderKey, protobufMimeType.toString()) // required by spring
                        .setHeader(protobufClassNameHeaderKey, payload::class.java.name) // required by converter logic
                        .build()
                }
                .orElse(null)
                ?.apply { log.info { "Convert protobuf => spring cloud message: $this" } }
    }

    companion object {
        const val protobufContentTypeHeaderKey = "contentType"
        val protobufMimeType = MimeType.valueOf("application/x-protobuf")
        const val protobufClassNameHeaderKey = "protobufClassName"

        private val log = logger()
        private val cache = ConcurrentReferenceHashMap<Class<*>, Method>()

        private val delegate =
            object : AbstractMessageConverter(protobufMimeType) {
                override fun supports(type: Class<*>): Boolean =
                    com.google.protobuf.Message::class.java.isAssignableFrom(type)
                        .apply { log.info { "Supports $type: $this" } }

                override fun convertFromInternal(message: Message<*>, type: Class<*>, unused: Any?): Any? {
                    val aTry = runCatching {
                        val builder = protobufBuilderOf(type)
                        builder.mergeFrom(message.payload as ByteArray).buildPartial()
                            .apply { builder.clear() }
                    }
                    if (aTry.isFailure) throw IllegalArgumentException(aTry.exceptionOrNull())
                    return aTry.getOrNull()
                        ?.apply { log.info { "Convert internal message: $message => $this" } }
                }

                override fun convertToInternal(payload: Any, headers: MessageHeaders?, unused: Any?): Any? =
                    com.google.protobuf.Message::class.java.cast(payload).toByteArray()
                        ?.apply { log.info { "Convert $payload => internal message: $this" } }
            }

        private fun protobufBuilderOf(type: Class<*>): com.google.protobuf.Message.Builder = run {
            val aTry = kotlin.runCatching {
                val method = cache.computeIfAbsent(type) { type.getMethod("newBuilder") }
                method.invoke(type) as com.google.protobuf.Message.Builder
            }
            if (aTry.isFailure)
                throw IllegalArgumentException(
                    "Cannot initiate com.google.protobuf.Message.Builder from $protobufClassNameHeaderKey header key",
                    aTry.exceptionOrNull()
                )
            aTry.getOrNull() ?: throw IllegalArgumentException("A com.google.protobuf.Message.Builder may not be null")
        }
    }
}
