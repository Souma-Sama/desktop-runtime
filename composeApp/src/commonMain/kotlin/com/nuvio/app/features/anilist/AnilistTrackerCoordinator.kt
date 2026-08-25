package com.nuvio.app.features.anilist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AnilistTrackerCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    // In-memory cache for media lookup: query -> AnilistMedia
    private val mediaCache = mutableMapOf<String, AnilistMedia>()

    private val _trackerState = MutableStateFlow(AnilistTrackerState())
    val trackerState: StateFlow<AnilistTrackerState> = _trackerState.asStateFlow()

    init {
        AnilistAuthRepository.ensureInitialized()
        scope.launch {
            AnilistAuthRepository.isAuthenticated.collect { isAuth ->
                _trackerState.update { it.copy(isAuthenticated = isAuth, user = AnilistAuthRepository.currentUser.value) }
            }
        }
    }

    fun loadForMedia(
        title: String,
        year: Int? = null,
        genres: List<String> = emptyList(),
    ) {
        val cleanTitle = cleanAnimeTitle(title)
        val cacheKey = "$cleanTitle:${year ?: 0}".lowercase()

        activeJob?.cancel()
        _trackerState.update {
            it.copy(
                isLoading = true,
                error = null,
                isAuthenticated = AnilistAuthRepository.isAuthenticated.value,
                user = AnilistAuthRepository.currentUser.value,
            )
        }

        activeJob = scope.launch {
            val token = AnilistAuthRepository.token.value
            val cachedMedia = mediaCache[cacheKey]

            val media = if (cachedMedia != null) {
                // If we have token, refresh entry data
                if (!token.isNullOrBlank()) {
                    AnilistApi.fetchMediaById(cachedMedia.id, token = token) ?: cachedMedia
                } else {
                    cachedMedia
                }
            } else {
                val searchResults = AnilistApi.searchAnime(query = cleanTitle, year = year, token = token)
                val bestMatch = searchResults.firstOrNull()
                if (bestMatch != null) {
                    mediaCache[cacheKey] = bestMatch
                }
                bestMatch
            }

            _trackerState.update {
                it.copy(
                    isLoading = false,
                    media = media,
                    entry = media?.mediaListEntry,
                    error = if (media == null) "No AniList match found" else null,
                )
            }
        }
    }

    fun updateStatus(status: AnilistMediaListStatus) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.saveMediaListEntry(
                mediaId = currentMedia.id,
                status = status,
                token = token,
            )
            _trackerState.update {
                it.copy(
                    isSyncing = false,
                    entry = updatedEntry ?: it.entry?.copy(status = status),
                )
            }
        }
    }

    fun updateProgress(progress: Int) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val maxEpisodes = currentMedia.episodes ?: Int.MAX_VALUE
        val safeProgress = progress.coerceIn(0, maxEpisodes)

        // Automatically set status to COMPLETED if reached max episodes
        val nextStatus = if (currentMedia.episodes != null && safeProgress >= currentMedia.episodes) {
            AnilistMediaListStatus.COMPLETED
        } else if (_trackerState.value.entry?.status == null || _trackerState.value.entry?.status == AnilistMediaListStatus.PLANNING) {
            AnilistMediaListStatus.CURRENT
        } else {
            _trackerState.value.entry?.status
        }

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.saveMediaListEntry(
                mediaId = currentMedia.id,
                status = nextStatus,
                progress = safeProgress,
                token = token,
            )
            _trackerState.update {
                it.copy(
                    isSyncing = false,
                    entry = updatedEntry ?: it.entry?.copy(progress = safeProgress, status = nextStatus),
                )
            }
        }
    }

    fun incrementProgress() {
        val currentProgress = _trackerState.value.entry?.progress ?: 0
        updateProgress(currentProgress + 1)
    }

    fun decrementProgress() {
        val currentProgress = _trackerState.value.entry?.progress ?: 0
        if (currentProgress > 0) {
            updateProgress(currentProgress - 1)
        }
    }

    fun updateScore(score: Double) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val safeScore = score.coerceIn(0.0, 10.0)

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.saveMediaListEntry(
                mediaId = currentMedia.id,
                score = safeScore,
                token = token,
            )
            _trackerState.update {
                it.copy(
                    isSyncing = false,
                    entry = updatedEntry ?: it.entry?.copy(score = safeScore),
                )
            }
        }
    }

    fun deleteEntry() {
        val entryId = _trackerState.value.entry?.id ?: return
        val token = AnilistAuthRepository.token.value ?: return

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val success = AnilistApi.deleteMediaListEntry(entryId = entryId, token = token)
            if (success) {
                _trackerState.update {
                    it.copy(isSyncing = false, entry = null)
                }
            } else {
                _trackerState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun syncNow() {
        val media = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val refreshed = AnilistApi.fetchMediaById(media.id, token = token)
            _trackerState.update {
                it.copy(
                    isSyncing = false,
                    media = refreshed ?: it.media,
                    entry = refreshed?.mediaListEntry ?: it.entry,
                )
            }
        }
    }

    private fun cleanAnimeTitle(raw: String): String {
        return raw
            .replace(Regex("""\((TV|Movie|OVA|ONA|Special)\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""Season \d+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""S\d+""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
