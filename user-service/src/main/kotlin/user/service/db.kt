package user.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentMap
import microservices.user.api.converters.toLocalDateTime
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.mapdb.serializer.GroupSerializer
import org.mapdb.serializer.GroupSerializerObjectArray
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Repository

data class User(
    val id: UUID = UUID.fromString("0-0-0-0-0"),
    val username: String = "",
    val createdAt: LocalDateTime = 0.toLocalDateTime(),
)

@Configuration
class UserStoreConfig {

    @Bean
    fun dbFilePath(): File =
        Paths.get(".", "target", "user.map.db")
            .normalize()
            .toAbsolutePath().run {
                parent.toFile().mkdirs();
                toFile()
            }

    @Bean(destroyMethod = "close")
    fun db(dbFilePath: File): DB =
        DBMaker.fileDB(dbFilePath)
            .checksumHeaderBypass()
            .allocateStartSize(1024)
            .allocateIncrement(1024)
            .fileChannelEnable()
            .fileMmapEnableIfSupported()
            .executorEnable()
            .closeOnJvmShutdown() // .closeOnJvmShutdownWeakReference()
            .fileLockDisable() // .fileLockWait(TimeUnit.SECONDS.toMillis(5))
            .cleanerHackEnable()
            .make()

    @Bean
    fun userSerializer(objectMapper: ObjectMapper): GroupSerializer<User> =
        object : GroupSerializerObjectArray<User>() {
            override fun serialize(closableAndFlushableDataOutputStream: DataOutput2, userToBeSerialized: User) {
                val jsonString = objectMapper.writeValueAsString(userToBeSerialized)
                closableAndFlushableDataOutputStream.writeUTF(jsonString)
            }

            override fun deserialize(dataInputStream: DataInput2, available: Int): User = run {
                val jsonString = dataInputStream.readUTF()
                objectMapper.readValue<User>(jsonString)
            }
        }

    @Bean(destroyMethod = "close")
    fun userStore(db: DB, userSerializer: GroupSerializer<User>): ConcurrentMap<UUID, User> =
        db.hashMap<UUID, User>("userStore", Serializer.UUID, userSerializer)
            .createOrOpen()
}

@Repository
data class UserRepository(private val userStore: ConcurrentMap<UUID, User>) {

    fun save(user: User): Optional<User> =
        run {
            if (user.id != UUID.fromString("0-0-0-0-0"))
                throw InvalidUserException(user.id)
            Optional.of(
                userStore.computeIfAbsent(UUID.randomUUID()) {
                    user.copy(id = it)
                        .copy(createdAt = LocalDateTime.now())
                }
            )
        }

    fun findAll(): List<User> =
        userStore.values.sortedByDescending { it.createdAt }

    fun findById(id: UUID): Optional<User> =
        Optional.ofNullable(userStore[id])

    fun deleteAllInBatch(): Unit =
        userStore.clear()
}
