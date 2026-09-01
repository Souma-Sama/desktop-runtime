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

        val hasAnimeId = extractAnilistId(mediaId) != null || extractMalId(mediaId) != null || extractKitsuId(mediaId) != null
        val isExplicitAnime = hasAnimeId || isAnimeCandidate(rawTitle, genres, country, language)
        val cacheKey = if (!mediaId.isNullOrBlank()) mediaId.lowercase() else rawTitle.lowercase()

        val isNewMedia = currentKey != cacheKey
        if (!forceRefresh && !isNewMedia && (_trackerState.value.media != null || activeJob?.isActive == true)) {
            return
        }
        currentKey = cacheKey

        log.i { "loadForMedia: title='$rawTitle', mediaId='$mediaId'" }

        activeJob?.cancel()
        _trackerState.update {
            it.copy(
                isLoading = true,
                isAnime = isExplicitAnime || (if (isNewMedia) false else it.media != null),
                media = if (isNewMedia) null else it.media,
                entry = if (isNewMedia) null else it.entry,
                error = null,
                isAuthenticated = AnilistAuthRepository.isAuthenticated.value,
                user = AnilistAuthRepository.currentUser.value,
                lastLookupTitle = rawTitle,
                lastLookupMediaId = mediaId,
            )
        }

        activeJob = scope.launch {
            val debug = StringBuilder()
            debug.appendLine("[Input] Title=\"$rawTitle\", MediaId=\"$mediaId\"")
            debug.appendLine("[Candidate] Anime Candidate: $isExplicitAnime (genres: $genres, country: $country)")
            var strategyUsed = "None"

            try {
                val token = AnilistAuthRepository.token.value
                val cachedMedia = mediaCache[cacheKey]

                val media = if (cachedMedia != null && !forceRefresh) {
                    strategyUsed = "Cache Hit (${cachedMedia.title?.displayTitle})"
                    debug.appendLine("[Cache] $strategyUsed")
                    if (!token.isNullOrBlank()) {
                        AnilistApi.fetchMediaById(cachedMedia.id, token = token, forceRefresh = false) ?: cachedMedia
                    } else {
                        cachedMedia
                    }
                } else if (cachedMedia != null && forceRefresh && !token.isNullOrBlank()) {
                    strategyUsed = "Cache Refresh (${cachedMedia.title?.displayTitle})"
                    debug.appendLine("[Cache Refresh] $strategyUsed")
                    AnilistApi.fetchMediaById(cachedMedia.id, token = token, forceRefresh = true) ?: cachedMedia
                } else {
                    var fetched: AnilistMedia? = null

                    // 1. Direct AniList ID lookup if mediaId contains an AniList ID
                    val anilistId = extractAnilistId(mediaId)
                    debug.appendLine("[Lookup] Extracted AniList ID: $anilistId")
                    if (anilistId != null) {
                        fetched = AnilistApi.fetchMediaById(anilistId, token = null)
                        if (fetched != null) {
                            strategyUsed = "Direct AniList ID #$anilistId -> ${fetched.title?.displayTitle}"
                            debug.appendLine("[Found] Via AniList ID #$anilistId: \"${fetched.title?.displayTitle}\"")
                        } else {
                            debug.appendLine("[Warning] AniList API returned null for ID #$anilistId. Response: ${AnilistApi.lastDebugLog}")
                        }
                    }

                    // 2. Direct MAL ID lookup if mediaId contains a MAL ID
                    if (fetched == null) {
                        val malId = extractMalId(mediaId)
                        debug.appendLine("[Lookup] Extracted MAL ID: $malId")
                        if (malId != null) {
                            fetched = AnilistApi.fetchMediaByMalId(malId, token = null)
                            if (fetched != null) {
                                strategyUsed = "Direct MAL ID #$malId -> ${fetched.title?.displayTitle}"
                                debug.appendLine("[Found] Via MAL ID #$malId: \"${fetched.title?.displayTitle}\"")
                            }
                        }
                    }

                    // 3. Direct Kitsu ID via ARM
                    if (fetched == null) {
                        val kitsuId = extractKitsuId(mediaId)
                        debug.appendLine("[Lookup] Extracted Kitsu ID: $kitsuId")
                        if (kitsuId != null) {
                            val armAnilistId = AnilistApi.resolveArmAnilistId(source = "kitsu", id = kitsuId)
                            debug.appendLine("[ARM] Kitsu -> AniList: $armAnilistId")
                            if (armAnilistId != null) {
                                fetched = AnilistApi.fetchMediaById(armAnilistId, token = null)
                                if (fetched != null) {
                                    strategyUsed = "Kitsu via ARM #$armAnilistId -> ${fetched.title?.displayTitle}"
                                }
                            }
                        }
                    }

                    // 4. Multi-Strategy Title Search (Public, 100% Reliable)
                    if (fetched == null && rawTitle.isNotBlank()) {
                        val candidates = generateSearchCandidates(rawTitle)
                        debug.appendLine("[Search] Candidates: $candidates")
                        for (query in candidates) {
                            val results = AnilistApi.searchAnime(query = query)
                            debug.appendLine("   - Search \"$query\" returned ${results.size} items: ${results.take(3).map { "${it.title?.displayTitle} (#${it.id})" }}")
                            if (results.isNotEmpty()) {
                                fetched = results.first()
                                strategyUsed = "Title Search \"$query\" -> #${fetched.id} ${fetched.title?.displayTitle}"
                                break
                            }
                        }
                    }

                    // 5. REST Kitsu -> ARM -> AniList Resolver Fallback
                    if (fetched == null && rawTitle.isNotBlank()) {
                        val candidates = generateSearchCandidates(rawTitle)
                        for (query in candidates) {
                            val resolvedAnilistId = AnilistApi.searchViaKitsu(query)
                            debug.appendLine("[Kitsu] Search \"$query\" -> AniList ID: $resolvedAnilistId")
                            if (resolvedAnilistId != null) {
                                fetched = AnilistApi.fetchMediaById(resolvedAnilistId, token = null)
                                if (fetched != null) {
                                    strategyUsed = "Kitsu Fallback -> #${fetched.id} ${fetched.title?.displayTitle}"
                                    break
                                }
                            }
                        }
                    }

                    // 6. Enrich with personal user list entry if logged in
                    if (fetched != null && !token.isNullOrBlank()) {
                        val enriched = AnilistApi.fetchMediaById(fetched.id, token = token)
                        if (enriched != null) {
                            fetched = enriched
                            debug.appendLine("[Sync] Enriched with user list status: ${enriched.mediaListEntry?.status}")
                        }
                    }

                    if (fetched != null) {
                        mediaCache[cacheKey] = fetched
                    }
                    fetched
                }

                val hasMatch = media != null
                debug.appendLine(if (hasMatch) "🎯 Final Match: #${media.id} ${media.title?.displayTitle}" else "❌ No match found on AniList.")

                val localLibraryItem = media?.let { m ->
                    AnilistLibraryRepository.getMediaEntry(m.id)
                        ?: m.title?.displayTitle?.let { AnilistLibraryRepository.getMediaEntryByTitle(it) }
                }
                val effectiveEntry = media?.mediaListEntry ?: localLibraryItem?.toMediaListEntry()
                val finalMedia = if (media != null && media.mediaListEntry == null && effectiveEntry != null) {
                    media.copy(mediaListEntry = effectiveEntry)
                } else {
                    media
                }
                if (finalMedia != null) {
                    mediaCache[cacheKey] = finalMedia
                    mediaCache["${finalMedia.id}"] = finalMedia
                    mediaCache["ani_${finalMedia.id}"] = finalMedia
                    mediaCache["anilist:${finalMedia.id}"] = finalMedia
                }

                _trackerState.update {
                    it.copy(
                        isLoading = false,
                        isAnime = hasMatch || isExplicitAnime,
                        media = finalMedia,
                        entry = effectiveEntry,
                        error = if (!hasMatch) "No matching anime found on AniList for \"$rawTitle\"" else null,
                        debugInfo = debug.toString(),
                        resolvedStrategy = strategyUsed,
                    )
                }
            } catch (t: Throwable) {
                debug.appendLine("💥 Exception: ${t.message}")
                _trackerState.update {
                    it.copy(
                        isLoading = false,
                        error = t.message ?: "Failed to load AniList data",
                        debugInfo = debug.toString(),
                    )
                }
            }
        }
    }

    private fun applyLocalEntryUpdate(entry: AnilistMediaListEntry?) {
        val currentMedia = _trackerState.value.media ?: return
        val updatedMedia = currentMedia.copy(mediaListEntry = entry)
        _trackerState.update {
            it.copy(
                media = updatedMedia,
                entry = entry,
            )
        }
        val cacheKey = currentKey
        if (cacheKey != null) {
            mediaCache[cacheKey] = updatedMedia
        }
        mediaCache["${currentMedia.id}"] = updatedMedia
        mediaCache["ani_${currentMedia.id}"] = updatedMedia
        mediaCache["anilist:${currentMedia.id}"] = updatedMedia
        currentMedia.title?.displayTitle?.let { title ->
            mediaCache[title.lowercase()] = updatedMedia
        }
        AnilistApi.updateCachedMediaEntry(currentMedia.id, entry)
    }

    fun updateStatus(status: AnilistMediaListStatus) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return

        AnilistLibraryRepository.updateItem(
            mediaId = currentMedia.id,
            title = currentMedia.title?.displayTitle,
            status = status,
            totalEpisodes = currentMedia.episodes,
            posterUrl = currentMedia.coverImage?.extraLarge ?: currentMedia.coverImage?.large,
        )

        val existingEntry = _trackerState.value.entry
        val optimisticEntry = existingEntry?.copy(status = status)
            ?: AnilistMediaListEntry(
                id = 0,
                mediaId = currentMedia.id,
                status = status,
                score = 0.0,
                progress = 0,
                repeat = 0,
                private = false,
                hiddenFromStatusLists = false,
                notes = null,
                startedAt = null,
                completedAt = null,
                updatedAt = com.nuvio.app.features.watchprogress.WatchProgressClock.nowEpochMs() / 1000L,
            )
        applyLocalEntryUpdate(optimisticEntry)

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                status = status,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
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

        AnilistLibraryRepository.updateItem(
            mediaId = currentMedia.id,
            title = currentMedia.title?.displayTitle,
            status = nextStatus,
            progress = safeProgress,
            totalEpisodes = currentMedia.episodes,
            posterUrl = currentMedia.coverImage?.extraLarge ?: currentMedia.coverImage?.large,
        )

        val existingEntry = _trackerState.value.entry
        val optimisticEntry = existingEntry?.copy(progress = safeProgress, status = nextStatus)
            ?: AnilistMediaListEntry(
                id = 0,
                mediaId = currentMedia.id,
                status = nextStatus,
                score = 0.0,
                progress = safeProgress,
                repeat = 0,
                private = false,
                hiddenFromStatusLists = false,
                notes = null,
                startedAt = null,
                completedAt = null,
                updatedAt = com.nuvio.app.features.watchprogress.WatchProgressClock.nowEpochMs() / 1000L,
            )
        applyLocalEntryUpdate(optimisticEntry)

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                status = nextStatus,
                progress = safeProgress,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun incrementProgress() {
        val currentProgress = _trackerState.value.entry?.progress ?: 0
        updateProgress(currentProgress + 1)
    }

    fun markEpisodeWatchedFromPlayback(
        title: String,
        mediaId: String?,
        season: Int?,
        episode: Int?,
        progressPercent: Float,
        onSuccess: ((Int) -> Unit)? = null,
    ) {
        val token = AnilistAuthRepository.token.value ?: return
        val prefs = AnilistPreferencesRepository.snapshot()
        if (!prefs.autoMarkEpisodeWatched) return
        if (progressPercent < prefs.watchedPercentageThreshold) return

        val targetEpisode = episode ?: 1
        val rawTitle = title.replace(Regex("""\r|\n"""), " ").replace(Regex("""\s+"""), " ").trim()

        scope.launch {
            try {
                var media = _trackerState.value.media
                if (media == null || (mediaId != null && !mediaId.contains("${media.id}"))) {
                    val anilistId = extractAnilistId(mediaId)
                    if (anilistId != null) {
                        media = AnilistApi.fetchMediaById(anilistId, token = token)
                    } else if (rawTitle.isNotBlank()) {
                        val candidates = generateSearchCandidates(rawTitle)
                        for (query in candidates) {
                            val results = AnilistApi.searchAnime(query = query, token = token)
                            if (results.isNotEmpty()) {
                                media = results.first()
                                break
                            }
                        }
                    }
                }

                if (media == null) return@launch

                val currentEntry = media.mediaListEntry ?: _trackerState.value.entry
                val currentProgress = currentEntry?.progress ?: 0

                if (targetEpisode > currentProgress) {
                    val maxEpisodes = media.episodes ?: Int.MAX_VALUE
                    val safeProgress = targetEpisode.coerceIn(0, maxEpisodes)

                    val nextStatus = if (prefs.autoCompleteOnLastEpisode && media.episodes != null && safeProgress >= media.episodes) {
                        AnilistMediaListStatus.COMPLETED
                    } else if (prefs.autoMoveToWatchingOnStart && (currentEntry?.status == null || currentEntry.status == AnilistMediaListStatus.PLANNING)) {
                        AnilistMediaListStatus.CURRENT
                    } else {
                        currentEntry?.status ?: AnilistMediaListStatus.CURRENT
                    }

                    val updatedEntry = AnilistApi.updateMediaListEntry(
                        mediaId = media.id,
                        entryId = currentEntry?.id?.takeIf { it > 0 },
                        status = nextStatus,
                        progress = safeProgress,
                        token = token,
                    )

                    if (updatedEntry != null) {
                        AnilistLibraryRepository.updateItem(
                            mediaId = media.id,
                            title = media.title?.displayTitle,
                            status = nextStatus,
                            progress = safeProgress,
                            totalEpisodes = media.episodes,
                            posterUrl = media.coverImage?.extraLarge ?: media.coverImage?.large,
                        )
                        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
                        applyLocalEntryUpdate(updatedEntry)
                        onSuccess?.invoke(safeProgress)
                    }
                }
            } catch (e: Exception) {
                log.w(e) { "Failed to auto-mark episode progress on AniList" }
            }
        }
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
        val score100 = if (safeScore > 0.0) safeScore * 10.0 else 0.0

        AnilistLibraryRepository.updateItem(
            mediaId = currentMedia.id,
            title = currentMedia.title?.displayTitle,
            score100 = score100,
            totalEpisodes = currentMedia.episodes,
            posterUrl = currentMedia.coverImage?.extraLarge ?: currentMedia.coverImage?.large,
        )

        val existingEntry = _trackerState.value.entry
        val optimisticEntry = existingEntry?.copy(score = score100)
            ?: AnilistMediaListEntry(
                id = 0,
                mediaId = currentMedia.id,
                status = AnilistMediaListStatus.CURRENT,
                score = score100,
                progress = 0,
                repeat = 0,
                private = false,
                hiddenFromStatusLists = false,
                notes = null,
                startedAt = null,
                completedAt = null,
                updatedAt = com.nuvio.app.features.watchprogress.WatchProgressClock.nowEpochMs() / 1000L,
            )
        applyLocalEntryUpdate(optimisticEntry)

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                score = safeScore,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updateRepeat(repeat: Int) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val safeRepeat = repeat.coerceAtLeast(0)
        val existingEntry = _trackerState.value.entry
        applyLocalEntryUpdate(existingEntry?.copy(repeat = safeRepeat))

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                repeat = safeRepeat,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updatePrivate(isPrivate: Boolean) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val existingEntry = _trackerState.value.entry
        applyLocalEntryUpdate(existingEntry?.copy(private = isPrivate))

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                private = isPrivate,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updateHiddenFromStatusLists(hidden: Boolean) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val existingEntry = _trackerState.value.entry
        applyLocalEntryUpdate(existingEntry?.copy(hiddenFromStatusLists = hidden))

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                hiddenFromStatusLists = hidden,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updateNotes(notes: String) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val existingEntry = _trackerState.value.entry
        applyLocalEntryUpdate(existingEntry?.copy(notes = notes))

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                notes = notes,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updateStartedAt(date: AnilistFuzzyDate?) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val existingEntry = _trackerState.value.entry
        applyLocalEntryUpdate(existingEntry?.copy(startedAt = date))

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                startedAt = date,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updateCompletedAt(date: AnilistFuzzyDate?) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return
        val existingEntry = _trackerState.value.entry
        applyLocalEntryUpdate(existingEntry?.copy(completedAt = date))

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                completedAt = date,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun updateFullEntry(
        status: AnilistMediaListStatus? = null,
        progress: Int? = null,
        score: Double? = null,
        repeat: Int? = null,
        private: Boolean? = null,
        hiddenFromStatusLists: Boolean? = null,
        notes: String? = null,
        startedAt: AnilistFuzzyDate? = null,
        completedAt: AnilistFuzzyDate? = null,
    ) {
        val currentMedia = _trackerState.value.media ?: return
        val token = AnilistAuthRepository.token.value ?: return

        AnilistLibraryRepository.updateItem(
            mediaId = currentMedia.id,
            title = currentMedia.title?.displayTitle,
            status = status,
            progress = progress,
            score100 = score?.let { it * 10.0 },
            totalEpisodes = currentMedia.episodes,
            posterUrl = currentMedia.coverImage?.extraLarge ?: currentMedia.coverImage?.large,
        )

        val existingEntry = _trackerState.value.entry
        val optimisticEntry = existingEntry?.copy(
            status = status ?: existingEntry.status,
            progress = progress ?: existingEntry.progress,
            score = score?.let { it * 10.0 } ?: existingEntry.score,
            repeat = repeat ?: existingEntry.repeat,
            private = private ?: existingEntry.private,
            hiddenFromStatusLists = hiddenFromStatusLists ?: existingEntry.hiddenFromStatusLists,
            notes = notes ?: existingEntry.notes,
            startedAt = startedAt ?: existingEntry.startedAt,
            completedAt = completedAt ?: existingEntry.completedAt,
        )
        applyLocalEntryUpdate(optimisticEntry)

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val updatedEntry = AnilistApi.updateMediaListEntry(
                mediaId = currentMedia.id,
                entryId = existingEntry?.id?.takeIf { it > 0 },
                status = status,
                progress = progress,
                score = score,
                repeat = repeat,
                private = private,
                hiddenFromStatusLists = hiddenFromStatusLists,
                notes = notes,
                startedAt = startedAt,
                completedAt = completedAt,
                token = token,
            )
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            if (updatedEntry != null) {
                applyLocalEntryUpdate(updatedEntry)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    fun deleteEntry() {
        val entryId = _trackerState.value.entry?.id ?: return
        val currentMedia = _trackerState.value.media
        val token = AnilistAuthRepository.token.value ?: return

        if (currentMedia != null) {
            AnilistLibraryRepository.removeItem(currentMedia.id)
        }
        applyLocalEntryUpdate(null)

        scope.launch {
            _trackerState.update { it.copy(isSyncing = true) }
            val success = AnilistApi.deleteMediaListEntry(entryId = entryId, token = token)
            if (success) {
                com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
                applyLocalEntryUpdate(null)
            }
            _trackerState.update { it.copy(isSyncing = false) }
        }
    }

    private fun AnilistLibraryItem.toMediaListEntry(): AnilistMediaListEntry =
        AnilistMediaListEntry(
            id = entryId,
            mediaId = id,
            status = AnilistMediaListStatus.fromString(status),
            score = score ?: 0.0,
            progress = progress,
            repeat = 0,
            private = false,
            hiddenFromStatusLists = false,
            notes = null,
            startedAt = null,
            completedAt = null,
            updatedAt = updatedAt,
        )

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

    fun extractAnilistId(mediaId: String?): Int? {
        if (mediaId.isNullOrBlank()) return null
        val id = mediaId.trim()

        // 1. KatalogAnime_HF format: "ani_140960_140960_50265" or "ani_140960" or "ani:140960"
        if (id.startsWith("ani_", ignoreCase = true) || id.startsWith("ani:", ignoreCase = true)) {
            val clean = id.removePrefix("ani_").removePrefix("ANI_").removePrefix("ani:").removePrefix("ANI:")
            val firstSegment = clean.substringBefore("_").substringBefore(":")
            val parsed = firstSegment.toIntOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        // 2. Prefixed forms: "anilist:140960", "al:140960", "anime:140960"
        if (id.startsWith("anilist:", ignoreCase = true)) return id.substringAfter(":").substringBefore("_").substringBefore(":").toIntOrNull()
        if (id.startsWith("anilist_", ignoreCase = true)) return id.substringAfter("_").substringBefore("_").substringBefore(":").toIntOrNull()
        if (id.startsWith("al:", ignoreCase = true)) return id.substringAfter(":").substringBefore("_").substringBefore(":").toIntOrNull()
        if (id.startsWith("al_", ignoreCase = true)) return id.substringAfter("_").substringBefore("_").substringBefore(":").toIntOrNull()
        if (id.startsWith("anime:", ignoreCase = true)) return id.substringAfter(":").substringBefore("_").substringBefore(":").toIntOrNull()

        // 3. IMDb compound: "tt15477980:140960" (IMDb:AnilistId)
        if (id.startsWith("tt", ignoreCase = true) && id.contains(":")) {
            val secondPart = id.substringAfter(":")
            val parsed = secondPart.toIntOrNull()
            if (parsed != null && parsed > 0) return parsed
        }

        // 4. Plain numeric ID that is NOT an IMDb ID (tt...) — treat as AniList ID
        if (id.all { it.isDigit() } && id.length in 1..8) return id.toIntOrNull()

        return null
    }

    fun extractMalId(mediaId: String?): Int? {
        if (mediaId.isNullOrBlank()) return null
        val id = mediaId.trim()
        if (id.startsWith("mal:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()
        if (id.startsWith("mal_", ignoreCase = true)) return id.substringAfter("_").toIntOrNull()
        if (id.startsWith("myanimelist:", ignoreCase = true)) return id.substringAfter(":").toIntOrNull()

        // In KatalogAnime_HF: "ani_<anilistId>_<providerId>_<malId>"
        if (id.startsWith("ani_", ignoreCase = true)) {
            val parts = id.removePrefix("ani_").removePrefix("ANI_").split("_")
            if (parts.size >= 3) {
                val mal = parts[2].toIntOrNull()
                if (mal != null && mal > 0) return mal
            }
        }
        return null
    }

    fun extractKitsuId(mediaId: String?): String? {
        if (mediaId.isNullOrBlank()) return null
        val id = mediaId.trim()
        if (id.startsWith("kitsu:", ignoreCase = true)) return id.substringAfter(":")
        if (id.startsWith("kitsu_", ignoreCase = true)) return id.substringAfter("_")
        return null
    }

    fun hasAnimeId(mediaId: String?): Boolean {
        if (mediaId.isNullOrBlank()) return false
        val id = mediaId.trim().lowercase()
        if (id.startsWith("ani_") || id.startsWith("ani:") || id.startsWith("anilist") || id.startsWith("al:") || id.startsWith("al_") || id.startsWith("kitsu") || id.startsWith("mal") || id.startsWith("anime:")) return true
        if (id.all { it.isDigit() } && id.length in 1..8) return true
        if (id.startsWith("tt") && id.contains(":")) return true
        return false
    }

    private fun generateSearchCandidates(rawTitle: String): List<String> {
        val list = mutableListOf<String>()
        val trimmed = rawTitle.trim()
        if (trimmed.isNotBlank()) list.add(trimmed)

        val clean = cleanAnimeTitle(trimmed)
        if (clean.isNotBlank() && !list.contains(clean)) list.add(clean)

        // Season-stripped candidate (e.g. "SPY x FAMILY Season 2" -> "SPY x FAMILY")
        val seasonStripped = clean
            .replace(Regex("""(?i)\s+(?:Season\s+\d+|S\d+|\d+(?:st|nd|rd|th)\s+Season|Part\s+\d+|Cour\s+\d+|Arc).*$"""), "")
            .trim()
        if (seasonStripped.length >= 3 && !list.contains(seasonStripped)) {
            list.add(seasonStripped)
        }

        // Normalize cross symbols (e.g. SPY x FAMILY vs SPY×FAMILY vs Hunter x Hunter)
        if (trimmed.contains("×") || trimmed.contains(" x ", ignoreCase = true)) {
            val normalizedX = trimmed.replace("×", "x")
            if (!list.contains(normalizedX)) list.add(normalizedX)
            val crossSymbol = trimmed.replace(Regex("""\s+[xX]\s+"""), "×")
            if (!list.contains(crossSymbol)) list.add(crossSymbol)
            val noX = seasonStripped.replace("×", "x")
            if (!list.contains(noX)) list.add(noX)
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
        mediaId: String? = null,
    ): Boolean {
        if (hasAnimeId(mediaId)) return true
        val g = genres.map { it.lowercase() }
        if (g.any { it.contains("anime") || it.contains("anim") }) return true
        // Check for Japanese Kanji, Hiragana, or Katakana in the title
        if (title.any { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9faf' }) return true
        val isJapan = country?.lowercase() in listOf("jp", "japan") || language?.lowercase() in listOf("ja", "japanese", "jpn")
        if (isJapan) return true
        return false
    }
}
