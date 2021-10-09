package microservices.user.api.converters

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

fun Number.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochSecond(this.toLong())
        .atZone(ZoneOffset.UTC)
        .toLocalDateTime()

fun LocalDateTime?.toTimestampOrDefault(defaultValue: Long = 0): Long =
    Optional.ofNullable(this)
        .map { it.toEpochSecond(ZoneOffset.UTC) }
        .orElse(defaultValue)
