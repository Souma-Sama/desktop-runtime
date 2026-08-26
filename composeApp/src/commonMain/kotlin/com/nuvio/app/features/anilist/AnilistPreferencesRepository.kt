package com.nuvio.app.features.anilist

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AnilistPreferencesRepository {
    private val _preferences = MutableStateFlow(AnilistPreferences())
    val preferences: StateFlow<AnilistPreferences> = _preferences.asStateFlow()

    private var initialized = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        val loaded = AnilistPreferencesStorage.load()
        if (loaded != null) {
            _preferences.value = loaded
        }
    }

    fun snapshot(): AnilistPreferences {
        ensureLoaded()
        return _preferences.value
    }

    fun setAutoMarkEpisodeWatched(enabled: Boolean) {
        updateAndPersist { it.copy(autoMarkEpisodeWatched = enabled) }
    }

    fun setWatchedPercentageThreshold(threshold: Int) {
        updateAndPersist { it.copy(watchedPercentageThreshold = threshold.coerceIn(50, 99)) }
    }

    fun setAutoMoveToWatchingOnStart(enabled: Boolean) {
        updateAndPersist { it.copy(autoMoveToWatchingOnStart = enabled) }
    }

    fun setAutoCompleteOnLastEpisode(enabled: Boolean) {
        updateAndPersist { it.copy(autoCompleteOnLastEpisode = enabled) }
    }

    fun setShowSyncNotification(enabled: Boolean) {
        updateAndPersist { it.copy(showSyncNotification = enabled) }
    }

    fun setPreferredTitleLanguage(language: AnilistTitleLanguage) {
        updateAndPersist { it.copy(preferredTitleLanguage = language) }
    }

    fun setPreferredScoreFormat(format: AnilistScoreFormat) {
        updateAndPersist { it.copy(preferredScoreFormat = format) }
    }

    private fun updateAndPersist(transform: (AnilistPreferences) -> AnilistPreferences) {
        ensureLoaded()
        _preferences.update { current ->
            val updated = transform(current)
            AnilistPreferencesStorage.save(updated)
            updated
        }
    }
}
