package user.client

import feign.Headers
import feign.Param
import feign.RequestLine
import user.api.UserApiProtos.UserDocument
import user.api.UserApiProtos.UsersDocument

@Headers("Content-Type: application/x-protobuf")
interface UserClient {

    @RequestLine("GET /api/v1/users/{id}")
    fun getUserById(@Param("id") id: String): UserDocument

    @RequestLine("GET /api/v1/users")
    fun getUsers(): UsersDocument
}
