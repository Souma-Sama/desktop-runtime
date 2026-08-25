package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
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
    private val log = Logger.withTag("AnilistTracker")
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

    private var currentKey: String? = null

    fun loadForMedia(
        title: String,
        mediaId: String? = null,
        year: Int? = null,
        genres: List<String> = emptyList(),
        country: String? = null,
        language: String? = null,
        forceRefresh: Boolean = false,
    ) {
        val rawTitle = title.replace(Regex("""\r|\n"""), " ").replace(Regex("""\s+"""), " ").trim()
        if (rawTitle.isBlank() && mediaId.isNullOrBlank()) return

        val isExplicitAnime = isAnimeCandidate(rawTitle, genres, country, language)
        val cacheKey = if (!mediaId.isNullOrBlank()) mediaId.lowercase() else rawTitle.lowercase()

        if (!forceRefresh && currentKey == cacheKey && (_trackerState.value.media != null || activeJob?.isActive == true)) {
            return
        }
        currentKey = cacheKey

        log.i { "loadForMedia: title='$rawTitle', mediaId='$mediaId'" }

        activeJob?.cancel()
        _trackerState.update {
            it.copy(
                isLoading = true,
                isAnime = isExplicitAnime || (it.media != null),
                error = null,
                isAuthenticated = AnilistAuthRepository.isAuthenticated.value,
                user = AnilistAuthRepository.currentUser.value,
            )
        }

        activeJob = scope.launch {
            try {
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
                    var fetched: AnilistMedia? = null

                    // 1. Direct AniList ID lookup if mediaId contains an AniList ID
                    val anilistId = extractAnilistId(mediaId)
                    if (anilistId != null) {
                        fetched = AnilistApi.fetchMediaById(anilistId, token = null)
                            ?: AnilistMedia(
                                id = anilistId,
                                title = AnilistTitle(
                                    english = rawTitle.ifBlank { "AniList #$anilistId" },
                                    romaji = rawTitle.ifBlank { "AniList #$anilistId" },
                                    native = rawTitle.ifBlank { "AniList #$anilistId" },
                                ),
                                format = "TV",
                            )
                    }

                    // 2. Direct MAL ID lookup if mediaId contains a MAL ID
                    if (fetched == null) {
                        val malId = extractMalId(mediaId)
                        if (malId != null) {
                            fetched = AnilistApi.fetchMediaByMalId(malId, token = null)
                        }
                    }

                    // 3. Direct Kitsu ID via ARM
                    if (fetched == null) {
                        val kitsuId = extractKitsuId(mediaId)
                        if (kitsuId != null) {
                            val armAnilistId = AnilistApi.resolveArmAnilistId(source = "kitsu", id = kitsuId)
                            if (armAnilistId != null) {
                                fetched = AnilistApi.fetchMediaById(armAnilistId, token = null)
                            }
                        }
                    }

                    // 4. Multi-Strategy Title Search (Public, 100% Reliable)
                    if (fetched == null && rawTitle.isNotBlank()) {
                        val candidates = generateSearchCandidates(rawTitle)
                        for (query in candidates) {
                            val results = AnilistApi.searchAnime(query = query)
                            if (results.isNotEmpty()) {
                                fetched = results.first()
                                break
                            }
                        }
                    }

                    // 5. REST Kitsu -> ARM -> AniList Resolver Fallback
                    if (fetched == null && rawTitle.isNotBlank()) {
                        val candidates = generateSearchCandidates(rawTitle)
                        for (query in candidates) {
                            val resolvedAnilistId = AnilistApi.searchViaKitsu(query)
                            if (resolvedAnilistId != null) {
                                fetched = AnilistApi.fetchMediaById(resolvedAnilistId, token = null)
                                    ?: AnilistMedia(
                                        id = resolvedAnilistId,
                                        title = AnilistTitle(english = query, romaji = query, native = query),
                                        format = "TV",
                                    )
                                break
                            }
                        }
                    }

                    // 6. Enrich with personal user list entry if logged in
                    if (fetched != null && !token.isNullOrBlank()) {
                        val enriched = AnilistApi.fetchMediaById(fetched.id, token = token)
                        if (enriched != null) {
                            fetched = enriched
                        }
                    }

                    if (fetched != null) {
                        mediaCache[cacheKey] = fetched
                    }
                    fetched
                }

                val hasMatch = media != null

                _trackerState.update {
                    it.copy(
                        isLoading = false,
                        isAnime = hasMatch || isExplicitAnime,
                        media = media,
                        entry = media?.mediaListEntry,
                        error = if (!hasMatch) "No matching anime found on AniList for \"$rawTitle\"" else null,
                    )
                }
            } catch (t: Throwable) {
                _trackerState.update {
                    it.copy(
                        isLoading = false,
                        error = t.message ?: "Failed to load AniList data",
                    )
                }
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

    private fun extractAnilistId(mediaId: String?): Int? {
        if (mediaId.isNullOrBlank()) return null
        val id = mediaId.trim()
        if (id.startsWith("anilist:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()
        if (id.startsWith("anilist_", ignoreCase = true)) return id.substringAfter("_").toIntOrNull()
        if (id.startsWith("al:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()
        if (id.startsWith("al_", ignoreCase = true)) return id.substringAfter("_").toIntOrNull()
        if (id.startsWith("anime:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()
        return null
    }

    private fun extractMalId(mediaId: String?): Int? {
        if (mediaId.isNullOrBlank()) return null
        val id = mediaId.trim()
        if (id.startsWith("mal:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()
        if (id.startsWith("mal_", ignoreCase = true)) return id.substringAfter("_").toIntOrNull()
        if (id.startsWith("myanimelist:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()
        return null
    }

    private fun extractKitsuId(mediaId: String?): String? {
        if (mediaId.isNullOrBlank()) return null
        val id = mediaId.trim()
        if (id.startsWith("kitsu:", ignoreCase = true)) return id.substringAfter(":")
        if (id.startsWith("kitsu_", ignoreCase = true)) return id.substringAfter("_")
        return null
    }

    private fun generateSearchCandidates(rawTitle: String): List<String> {
        val list = mutableListOf<String>()
        val trimmed = rawTitle.trim()
        if (trimmed.isNotBlank()) list.add(trimmed)

        val clean = cleanAnimeTitle(trimmed)
        if (clean.isNotBlank() && !list.contains(clean)) list.add(clean)

        // Normalize cross symbols (e.g. SPY x FAMILY vs SPY×FAMILY vs Hunter x Hunter)
        if (trimmed.contains("×") || trimmed.contains(" x ", ignoreCase = true)) {
            val normalizedX = trimmed.replace("×", "x")
            if (!list.contains(normalizedX)) list.add(normalizedX)
            val crossSymbol = trimmed.replace(Regex("""\s+[xX]\s+"""), "×")
            if (!list.contains(crossSymbol)) list.add(crossSymbol)
        }

        if (trimmed.contains(":")) {
            val beforeColon = cleanAnimeTitle(trimmed.substringBefore(":")).trim()
            if (beforeColon.length >= 3 && !list.contains(beforeColon)) list.add(beforeColon)
            val afterColon = cleanAnimeTitle(trimmed.substringAfter(":")).trim()
            if (afterColon.length >= 3 && !list.contains(afterColon)) list.add(afterColon)
        }

        if (trimmed.contains(" - ")) {
            val beforeDash = cleanAnimeTitle(trimmed.substringBefore(" - ")).trim()
            if (beforeDash.length >= 3 && !list.contains(beforeDash)) list.add(beforeDash)
        }

        return list
    }

    private fun cleanAnimeTitle(raw: String): String {
        return raw
            .replace(Regex("""\r|\n"""), " ")
            .replace(Regex("""\[(Dub|Sub|Dual Audio|Multi-Audio|1080p|720p|4K|HEVC|x264|x265)\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\((Dub|Sub|Dual Audio|Multi-Audio|TV|Movie|OVA|ONA|Special)\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
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

    fun isAnimeCandidate(
        title: String,
        genres: List<String>,
        country: String?,
        language: String?,
    ): Boolean {
        val g = genres.map { it.lowercase() }
        if (g.any { it.contains("anime") || it.contains("anim") }) return true
        // Check for Japanese Kanji, Hiragana, or Katakana in the title
        if (title.any { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9faf' }) return true
        val isJapan = country?.lowercase() in listOf("jp", "japan") || language?.lowercase() in listOf("ja", "japanese", "jpn")
        if (isJapan) return true
        return false
    }
}
