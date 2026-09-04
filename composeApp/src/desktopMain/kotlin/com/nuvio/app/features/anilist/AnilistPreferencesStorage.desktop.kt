package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal actual object AnilistPreferencesStorage {
    private const val PREFS_KEY = "anilist_preferences_payload"
    private val json = Json { ignoreUnknownKeys = true }
    private val store = DesktopStorage.store("nuvio_anilist_preferences")

    actual fun load(): AnilistPreferences? {
        val raw = store.getString(ProfileScopedKey.of(PREFS_KEY)) ?: return null
        return runCatching { json.decodeFromString<AnilistPreferences>(raw) }.getOrNull()
    }

    actual fun save(preferences: AnilistPreferences) {
        runCatching {
            store.putString(ProfileScopedKey.of(PREFS_KEY), json.encodeToString(preferences))
        }
    }
}
