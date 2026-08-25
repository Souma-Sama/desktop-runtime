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
        country: String? = null,
        language: String? = null,
    ) {
        val cleanTitle = cleanAnimeTitle(title)
        val isExplicitAnime = isAnimeCandidate(cleanTitle, genres, country, language)
        val cacheKey = "$cleanTitle:${year ?: 0}".lowercase()

        activeJob?.cancel()
        _trackerState.update {
            it.copy(
                isLoading = true,
                isAnime = isExplicitAnime,
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
                var searchResults = AnilistApi.searchAnime(query = cleanTitle, year = year, token = token)
                if (searchResults.isEmpty() && year != null) {
                    searchResults = AnilistApi.searchAnime(query = cleanTitle, year = null, token = token)
                }
                if (searchResults.isEmpty() && cleanTitle != title) {
                    searchResults = AnilistApi.searchAnime(query = title.trim(), year = null, token = token)
                }

                val bestMatch = searchResults.firstOrNull { candidate ->
                    isTitleMatch(cleanTitle, title, candidate)
                }
                if (bestMatch != null) {
                    mediaCache[cacheKey] = bestMatch
                }
                bestMatch
            }

            val isConfirmedAnime = (media != null && (isExplicitAnime || isTitleMatch(cleanTitle, title, media)))

            _trackerState.update {
                it.copy(
                    isLoading = false,
                    isAnime = isConfirmedAnime,
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

    private fun isTitleMatch(cleanTitle: String, rawTitle: String, media: AnilistMedia): Boolean {
        val queryClean = cleanTitle.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
        val rawClean = rawTitle.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }.trim()
        val queryTokens = queryClean.split(" ").filter { it.isNotBlank() }

        val candidateTitles = listOfNotNull(
            media.title?.english?.lowercase(),
            media.title?.romaji?.lowercase(),
            media.title?.native?.lowercase(),
        ).map { it.filter { char -> char.isLetterOrDigit() || char.isWhitespace() }.trim() }

        if (queryTokens.isEmpty()) return false

        return candidateTitles.any { candidateTitle ->
            if (candidateTitle.isBlank()) return@any false
            if (candidateTitle == queryClean || candidateTitle == rawClean) return@any true
            if (candidateTitle.contains(queryClean) || queryClean.contains(candidateTitle)) return@any true
            val candidateTokens = candidateTitle.split(" ").filter { it.isNotBlank() }
            val matchCount = queryTokens.count { token -> candidateTokens.contains(token) }
            val ratio = matchCount.toDouble() / queryTokens.size.toDouble()
            ratio >= 0.5
        }
    }

    private fun isAnimeCandidate(
        title: String,
        genres: List<String>,
        country: String?,
        language: String?,
    ): Boolean {
        val g = genres.map { it.lowercase() }
        if (g.any { it.contains("anime") }) return true
        val isJapan = country?.lowercase() in listOf("jp", "japan") || language?.lowercase() in listOf("ja", "japanese", "jpn")
        if (isJapan && g.any { it.contains("anim") }) return true
        return false
    }
}
