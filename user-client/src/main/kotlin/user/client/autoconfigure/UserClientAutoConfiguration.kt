package user.client.autoconfigure

import feign.Feign
import feign.Logger.Level.FULL
import feign.codec.Decoder
import feign.codec.Encoder
import feign.slf4j.Slf4jLogger
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.cloud.openfeign.support.SpringEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import user.client.UserClient

@ConstructorBinding
@ConfigurationProperties("user")
data class UserProps(
    val host: String = "undefined",
    val port: Int = -1,
)

@Configuration
@EnableConfigurationProperties(UserProps::class)
class UserClientAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(Encoder::class)
    fun userClientEncoder(messageConverters: ObjectFactory<HttpMessageConverters>): Encoder =
        SpringEncoder(messageConverters)

    @Bean
    @Primary
    @ConditionalOnMissingBean(Decoder::class)
    fun userClientDecoder(messageConverters: ObjectFactory<HttpMessageConverters>): Decoder =
        ResponseEntityDecoder(SpringDecoder(messageConverters))

    @Bean
    @Primary
    @Suppress("HttpUrlsUsage")
    @ConditionalOnMissingBean(UserClient::class)
    fun userClient(
        userClientEncoder: Encoder,
        userClientDecoder: Decoder,
        userProps: UserProps,
    ): UserClient =
        Feign.builder()
            .logger(Slf4jLogger())
            .logLevel(FULL)
            .encoder(userClientEncoder)
            .decoder(userClientDecoder)
            .target(UserClient::class.java, "http://${userProps.host}:${userProps.port}")
}
