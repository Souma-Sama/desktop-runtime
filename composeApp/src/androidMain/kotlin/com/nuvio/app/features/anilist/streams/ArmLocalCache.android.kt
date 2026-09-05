package com.nuvio.app.features.anilist.streams

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.ArmMapping

internal actual object ArmLocalCache {
    private const val PREFS_NAME = "nuvio_arm_cache"
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun get(anilistId: Int): ArmMapping? {
        val raw = preferences?.getString(anilistId.toString(), null) ?: return null
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
        preferences?.edit()?.putString(anilistId.toString(), raw)?.apply()
    }
}
