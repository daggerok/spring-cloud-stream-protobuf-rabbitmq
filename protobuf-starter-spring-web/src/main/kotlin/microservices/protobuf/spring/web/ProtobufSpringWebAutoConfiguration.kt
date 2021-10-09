package microservices.protobuf.spring.web

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter

@Configuration
@ConditionalOnClass(com.google.protobuf.Message::class)
class ProtobufSpringWebAutoConfiguration {

    @Bean
    @Primary
    // @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(ProtobufHttpMessageConverter::class)
    fun protobufHttpMessageConverter(): ProtobufHttpMessageConverter =
        ProtobufHttpMessageConverter()
}
