package com.nuvio.app.features.anilist.streams

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.ArmMapping

internal actual object ArmLocalCache {
    private val store = DesktopStorage.store("nuvio_arm_cache")

    actual fun get(anilistId: Int): ArmMapping? {
        val raw = store.getString(anilistId.toString()) ?: return null
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
        store.putString(anilistId.toString(), raw)
    }
}
