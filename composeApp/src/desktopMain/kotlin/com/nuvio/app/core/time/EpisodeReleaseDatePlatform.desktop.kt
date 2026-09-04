package com.nuvio.app.core.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal actual object EpisodeReleaseDatePlatform {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()

    actual fun localIsoDateAtEpochMs(epochMs: Long): String? = runCatching {
        Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }.getOrNull()

    actual fun localTimeAtEpochMs(epochMs: Long): String? = runCatching {
        val time = Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val h = time.hour.toString().padStart(2, '0')
        val m = time.minute.toString().padStart(2, '0')
        "$h:$m"
    }.getOrNull()

    actual fun localDateTimeToEpochMs(normalizedIsoDateTime: String): Long? = runCatching {
        LocalDateTime.parse(normalizedIsoDateTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}
