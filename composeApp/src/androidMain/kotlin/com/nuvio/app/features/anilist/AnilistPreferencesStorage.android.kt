package com.nuvio.app.features.anilist

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal actual object AnilistPreferencesStorage {
    private const val PREFS_NAME = "nuvio_anilist_preferences"
    private const val PREFS_KEY = "anilist_preferences_payload"
    private val json = Json { ignoreUnknownKeys = true }
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun load(): AnilistPreferences? {
        val raw = preferences?.getString(PREFS_KEY, null) ?: return null
        return runCatching { json.decodeFromString<AnilistPreferences>(raw) }.getOrNull()
    }

    actual fun save(preferences: AnilistPreferences) {
        this.preferences?.edit()?.apply {
            runCatching {
                putString(PREFS_KEY, json.encodeToString(preferences))
            }
            apply()
        }
    }
}
