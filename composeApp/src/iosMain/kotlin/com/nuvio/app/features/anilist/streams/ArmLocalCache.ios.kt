package com.nuvio.app.features.anilist.streams

import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.ArmMapping
import platform.Foundation.NSUserDefaults

internal actual object ArmLocalCache {
    private const val PREFIX = "nuvio_arm_"
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun get(anilistId: Int): ArmMapping? {
        val raw = defaults.stringForKey("$PREFIX$anilistId") ?: return null
        val parts = raw.split(':')
        if (parts.size < 6) return null
        return ArmMapping(
            imdbId = parts[0].ifEmpty { null },
            kitsuId = parts[1].ifEmpty { null },
            tmdbId = parts[2].toIntOrNull(),
            tvdbId = parts[3].ifEmpty { null },
            malId = parts[4].toIntOrNull(),
            season = parts.getOrNull(5)?.toIntOrNull() ?: 1,
        )
    }

    actual fun put(anilistId: Int, mapping: ArmMapping) {
        val raw = "${mapping.imdbId.orEmpty()}:${mapping.kitsuId.orEmpty()}:${mapping.tmdbId ?: ""}:${mapping.tvdbId.orEmpty()}:${mapping.malId ?: ""}:${mapping.season}"
        defaults.setObject(raw, forKey = "$PREFIX$anilistId")
    }
}
