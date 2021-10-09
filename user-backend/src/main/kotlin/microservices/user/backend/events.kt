package microservices.user.backend

import microservices.protobuf.spring.cloud.stream.ProtobufSpringCloudStreamAutoConfiguration.Companion.protobufMimeType
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import user.api.UserApiProtos.CreateUserEvent

@Service
class EvenStream(private val streamBridge: StreamBridge) {

    fun send(event: CreateUserEvent) {
        streamBridge.send("createUserEventStream-out-0", event, protobufMimeType)
    }
}
