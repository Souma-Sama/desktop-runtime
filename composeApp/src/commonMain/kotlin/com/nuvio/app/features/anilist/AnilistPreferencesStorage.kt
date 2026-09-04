package com.nuvio.app.features.anilist

internal expect object AnilistPreferencesStorage {
    fun load(): AnilistPreferences?
    fun save(preferences: AnilistPreferences)
}
