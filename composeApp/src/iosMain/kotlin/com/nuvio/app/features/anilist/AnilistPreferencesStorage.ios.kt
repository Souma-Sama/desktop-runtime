package com.nuvio.app.features.anilist

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

internal actual object AnilistPreferencesStorage {
    private const val PREFS_KEY = "nuvio_anilist_preferences_payload"
    private val json = Json { ignoreUnknownKeys = true }
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AnilistPreferences? {
        val raw = defaults.stringForKey(PREFS_KEY) ?: return null
        return runCatching { json.decodeFromString<AnilistPreferences>(raw) }.getOrNull()
    }

    actual fun save(preferences: AnilistPreferences) {
        runCatching {
            defaults.setObject(json.encodeToString(preferences), forKey = PREFS_KEY)
        }
    }
}
