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

    fun setEnabled(enabled: Boolean) {
        updateAndPersist { it.copy(enabled = enabled) }
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

    fun setAutoAddNewAnime(enabled: Boolean) {
        updateAndPersist { it.copy(autoAddNewAnime = enabled) }
    }

    fun setPosterRatingBadgeScale(scale: Float) {
        updateAndPersist { it.copy(posterRatingBadgeScale = scale.coerceIn(0.6f, 1.6f)) }
    }

    fun setPosterStatusBadgeScale(scale: Float) {
        updateAndPersist { it.copy(posterStatusBadgeScale = scale.coerceIn(0.6f, 1.6f)) }
    }

    fun setPosterTitleLogoScale(scale: Float) {
        updateAndPersist { it.copy(posterTitleLogoScale = scale.coerceIn(0.6f, 1.6f)) }
    }

    fun setHeroTitleLogoScale(scale: Float) {
        updateAndPersist { it.copy(heroTitleLogoScale = scale.coerceIn(0.6f, 1.8f)) }
    }

    fun setStreamIdOverride(anilistId: Int, overrideValue: String?) {
        updateAndPersist { current ->
            val updated = current.streamIdOverrides.toMutableMap()
            if (overrideValue != null) {
                updated[anilistId] = overrideValue
            } else {
                updated.remove(anilistId)
            }
            current.copy(streamIdOverrides = updated)
        }
    }

    fun getEffectiveSections(isAuthenticated: Boolean): List<AnilistSectionSettings> {
        ensureLoaded()
        val savedSections = _preferences.value.librarySections
        val baseSections = if (isAuthenticated) {
            AnilistPreferences.defaultAuthenticatedSections
        } else {
            AnilistPreferences.defaultUnauthenticatedSections
        }

        val result = mutableListOf<AnilistSectionSettings>()
        savedSections.forEach { saved ->
            val match = baseSections.firstOrNull { it.type.equals(saved.type, ignoreCase = true) || isSectionAlias(it.type, saved.type) }
            if (match != null && result.none { it.type.equals(match.type, ignoreCase = true) }) {
                result.add(AnilistSectionSettings(type = match.type, enabled = saved.enabled))
            }
        }
        baseSections.forEach { base ->
            if (result.none { it.type.equals(base.type, ignoreCase = true) }) {
                result.add(base)
            }
        }
        return result
    }

    private fun isSectionAlias(a: String, b: String): Boolean {
        val cleanA = a.lowercase().trim()
        val cleanB = b.lowercase().trim()
        if (cleanA == cleanB) return true
        if ((cleanA == "watching" || cleanA == "currently watching") && (cleanB == "watching" || cleanB == "currently watching")) return true
        if ((cleanA == "planning" || cleanA == "plan to watch") && (cleanB == "planning" || cleanB == "plan to watch")) return true
        if ((cleanA == "trending" || cleanA == "trending anime") && (cleanB == "trending" || cleanB == "trending anime")) return true
        if ((cleanA == "airing" || cleanA == "currently airing") && (cleanB == "airing" || cleanB == "currently airing")) return true
        if ((cleanA == "popular" || cleanA == "popular this season") && (cleanB == "popular" || cleanB == "popular this season")) return true
        if ((cleanA == "top rated" || cleanA == "top rated anime") && (cleanB == "top rated" || cleanB == "top rated anime")) return true
        return false
    }

    fun setSectionEnabled(type: String, enabled: Boolean, isAuthenticated: Boolean = true) {
        updateAndPersist { current ->
            val currentSections = getEffectiveSections(isAuthenticated).toMutableList()
            val index = currentSections.indexOfFirst { it.type.equals(type, ignoreCase = true) || isSectionAlias(it.type, type) }
            if (index != -1) {
                currentSections[index] = currentSections[index].copy(enabled = enabled)
                current.copy(librarySections = currentSections)
            } else {
                current
            }
        }
    }

    fun moveSection(fromIndex: Int, toIndex: Int, isAuthenticated: Boolean = true) {
        updateAndPersist { current ->
            val currentSections = getEffectiveSections(isAuthenticated).toMutableList()
            if (fromIndex in currentSections.indices && toIndex in currentSections.indices) {
                val item = currentSections.removeAt(fromIndex)
                currentSections.add(toIndex, item)
                current.copy(librarySections = currentSections)
            } else {
                current
            }
        }
    }

    fun resetLibrarySections(isAuthenticated: Boolean = true) {
        val defaults = if (isAuthenticated) AnilistPreferences.defaultAuthenticatedSections else AnilistPreferences.defaultUnauthenticatedSections
        updateAndPersist { it.copy(librarySections = defaults) }
    }

    fun setPreferredTitleLanguage(language: AnilistTitleLanguage) {
        updateAndPersist { it.copy(preferredTitleLanguage = language) }
        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
        com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.clearCache()
        com.nuvio.app.features.home.HomeRepository.refresh(force = true)
    }

    fun setPreferredScoreFormat(format: AnilistScoreFormat) {
        updateAndPersist { it.copy(preferredScoreFormat = format) }
    }

    fun setShowPosterTitleLogos(enabled: Boolean) {
        updateAndPersist { it.copy(showPosterTitleLogos = enabled) }
    }

    fun setShowPosterAnilistScore(enabled: Boolean) {
        updateAndPersist { it.copy(showPosterAnilistScore = enabled) }
    }

    fun setShowPosterMalScore(enabled: Boolean) {
        updateAndPersist { it.copy(showPosterMalScore = enabled) }
    }

    fun setPosterScoreFormat(format: AnilistPosterScoreFormat) {
        updateAndPersist { it.copy(posterScoreFormat = format) }
    }

    fun setShowPosterStatusBadge(enabled: Boolean) {
        updateAndPersist { it.copy(showPosterStatusBadge = enabled) }
    }

    fun setEnableAdvancedFilters(enabled: Boolean) {
        updateAndPersist { it.copy(enableAdvancedFilters = enabled) }
    }

    fun setEnableStatsDashboard(enabled: Boolean) {
        updateAndPersist { it.copy(enableStatsDashboard = enabled) }
    }

    fun setEnableActivityFeed(enabled: Boolean) {
        updateAndPersist { it.copy(enableActivityFeed = enabled) }
    }

    fun setEnableInLibraryFilter(enabled: Boolean) {
        updateAndPersist { it.copy(enableInLibraryFilter = enabled) }
    }

    fun setEnableEpisodicDiscussions(enabled: Boolean) {
        updateAndPersist { it.copy(enableEpisodicDiscussions = enabled) }
    }

    fun setHideAdultContent(enabled: Boolean) {
        updateAndPersist { it.copy(hideAdultContent = enabled) }
        com.nuvio.app.features.anilist.calendar.CalendarRepository.clearCache()
    }

    fun setUseFloatingGlassDesktopSidebar(enabled: Boolean) {
        updateAndPersist { it.copy(useFloatingGlassDesktopSidebar = enabled) }
    }

    fun setTrackerTheme(theme: AnilistTrackerTheme) {
        updateAndPersist { it.copy(trackerTheme = theme) }
    }

    fun enqueuePendingScrobble(mutation: PendingScrobbleMutation) {
        updateAndPersist { current ->
            val filtered = current.pendingScrobbleMutations.filterNot { it.mediaId == mutation.mediaId }
            current.copy(pendingScrobbleMutations = filtered + mutation)
        }
    }

    fun removePendingScrobble(mediaId: Int) {
        updateAndPersist { current ->
            current.copy(pendingScrobbleMutations = current.pendingScrobbleMutations.filterNot { it.mediaId == mediaId })
        }
    }

    private fun updateAndPersist(transform: (AnilistPreferences) -> AnilistPreferences) {
        ensureLoaded()
        _preferences.update { current ->
            val updated = transform(current)
            AnilistPreferencesStorage.save(updated)
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            updated
        }
    }
}
