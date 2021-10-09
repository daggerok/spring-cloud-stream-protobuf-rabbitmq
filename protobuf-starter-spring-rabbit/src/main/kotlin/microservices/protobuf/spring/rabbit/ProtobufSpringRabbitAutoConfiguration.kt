package microservices.protobuf.spring.rabbit

import java.lang.reflect.Method
import org.apache.logging.log4j.kotlin.logger
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.util.MimeType

@Configuration
@ConditionalOnClass(com.google.protobuf.Message::class)
class ProtobufSpringRabbitAutoConfiguration {

    @Bean
    @Primary
    // @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(org.springframework.amqp.support.converter.AbstractMessageConverter::class)
    fun protobufAmqpMessageConverter(): org.springframework.amqp.support.converter.AbstractMessageConverter {
        return object : org.springframework.amqp.support.converter.AbstractMessageConverter() {
            override fun fromMessage(message: org.springframework.amqp.core.Message): Any {
                if (!com.google.protobuf.Message::class.java.isAssignableFrom(message.javaClass))
                    throw MessageConversionException("Message is not protobuf: $message")
                val contentType = message.messageProperties.contentType
                val isProtobuf = contentType.contains(protobufMimeType.toString())
                if (!isProtobuf) throw MessageConversionException("Message contentType is not protobuf: $contentType")
                val result = runCatching {
                    val protobufClassName = message.messageProperties.headers[protobufClassNameHeaderKey].toString()
                    val protobufClass = Class.forName(protobufClassName)
                    val builder = protobufBuilderOf(protobufClass)
                    builder.mergeFrom(message.body).buildPartial()
                        .apply { builder.clear() }
                }
                if (result.isFailure) throw IllegalArgumentException(
                    "Could not initiate Message.Builder from $protobufClassNameHeaderKey header key",
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
                props.contentType = protobufMimeType.toString()
                props.contentLength = byteArray.size.toLong()
                props.headers[protobufClassNameHeaderKey] = obj::class.java.name
                return org.springframework.amqp.core.Message(byteArray, props)
                    .apply { log.info { "Convert protobuf into AMQP message: $this" } }
            }
        }
    }

    companion object {
        val protobufMimeType = MimeType.valueOf("application/x-protobuf")
        const val protobufClassNameHeaderKey = "protobufClassName"

        private val log = logger()
        private val cache = ConcurrentReferenceHashMap<Class<*>, Method>()

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
