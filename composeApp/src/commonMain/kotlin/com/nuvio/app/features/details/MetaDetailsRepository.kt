package com.nuvio.app.features.details

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.buildAddonResourceUrl
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.fetchAddonResponseText
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.filterReleasedItems
import com.nuvio.app.features.mdblist.MdbListMetadataService
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.tmdb.TmdbMetadataService
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktConnectionMode
import com.nuvio.app.features.trakt.TraktRelatedRepository
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.trakt.shouldUseTraktMoreLikeThis
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

object MetaDetailsRepository {
    private data class CachedMetaEntry(
        val baseMeta: MetaDetails,
        val metaScreenMeta: MetaDetails? = null,
        val metaScreenSettingsFingerprint: String? = null,
    )

    private val log = Logger.withTag("MetaDetailsRepo")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _uiState = MutableStateFlow(MetaDetailsUiState())
    val uiState: StateFlow<MetaDetailsUiState> = _uiState.asStateFlow()
    private var activeRequestKey: String? = null
    private val cachedMetaByRequestKey = mutableMapOf<String, CachedMetaEntry>()

    fun load(type: String, id: String) {
        log.d { "load() called — type=$type id=$id" }
        val requestKey = "$type:$id"
        val currentState = _uiState.value
        val mdbListSettings = MdbListSettingsRepository.snapshot()
        val metaScreenSettingsFingerprint = buildMetaScreenSettingsFingerprint(mdbListSettings)

        cachedMetaByRequestKey[requestKey]?.let { cachedEntry ->
            cachedEntry.metaScreenMeta
                ?.takeIf { cachedEntry.metaScreenSettingsFingerprint == metaScreenSettingsFingerprint }
                ?.let { cachedMeta ->
                    _uiState.value = MetaDetailsUiState(meta = cachedMeta.withUnreleasedFilter())
                    activeRequestKey = requestKey
                    return
                }

            val cachedBaseMeta = cachedEntry.baseMeta
            if (!shouldEnrichForMetaScreen(cachedBaseMeta, id, mdbListSettings)) {
                _uiState.value = MetaDetailsUiState(meta = cachedBaseMeta.withUnreleasedFilter())
                activeRequestKey = requestKey
                return
            }

            if (currentState.isLoading && activeRequestKey == requestKey) {
                log.d { "Meta screen enrichment already in flight — type=$type id=$id" }
                return
            }

            activeRequestKey = requestKey
            _uiState.value = MetaDetailsUiState(
                isLoading = false,
                meta = cachedBaseMeta.withUnreleasedFilter(),
            )

            scope.launch {
                val lookupId = resolveMetaLookupId(id, type)
                val normalizedType = if (type == "movie") "movie" else "series"
                val enrichedMeta = withContext(Dispatchers.Default) {
                    enrichForMetaScreen(
                        requestKey = requestKey,
                        meta = cachedBaseMeta,
                        fallbackItemId = lookupId,
                        fallbackItemType = normalizedType,
                        settings = mdbListSettings,
                        settingsFingerprint = metaScreenSettingsFingerprint,
                    )
                }
                _uiState.value = MetaDetailsUiState(meta = enrichedMeta.withUnreleasedFilter(), isLoading = false)
                activeRequestKey = requestKey
            }
            return
        }

        if (currentState.meta?.type == type && currentState.meta.id == id && !currentState.isLoading) {
            log.d { "Skipping reload for cached meta — type=$type id=$id" }
            activeRequestKey = requestKey
            return
        }

        if (currentState.isLoading && activeRequestKey == requestKey) {
            log.d { "Request already in flight — type=$type id=$id" }
            return
        }

        activeRequestKey = requestKey
        _uiState.value = MetaDetailsUiState(isLoading = true)

        scope.launch {
            val isAnilistItem = id.startsWith("ani_", ignoreCase = true) || id.startsWith("anilist:", ignoreCase = true)
            val effectiveType = if (type == "movie") "movie" else "series"

            val anilistId = if (isAnilistItem) {
                com.nuvio.app.features.anilist.AnilistTrackerCoordinator.extractAnilistId(id)
            } else null

            if (anilistId != null) {
                val anilistMeta = runCatching {
                    com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.resolveMetaDetails("ani_$anilistId")
                }.getOrNull()
                if (anilistMeta != null) {
                    val metaLookupId = resolveMetaLookupId(itemId = "ani_$anilistId", itemType = type)
                    publishLoadedMeta(
                        requestKey = requestKey,
                        meta = anilistMeta,
                        fallbackItemId = metaLookupId,
                        fallbackItemType = effectiveType,
                        mdbListSettings = mdbListSettings,
                        metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
                    )
                    return@launch
                }
            }

            val metaLookupId = resolveMetaLookupId(itemId = id, itemType = type)
            val manifests = findReadyMetaManifests(type = effectiveType, id = metaLookupId)

            for (manifest in manifests) {
                val result = withContext(Dispatchers.Default) {
                    tryFetchMeta(manifest, effectiveType, metaLookupId, includeMdbList = false, enrichWithTmdb = false)
                }
                if (result != null) {
                    val finalMeta = if (isAnilistItem) {
                        com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.adaptCinemetaForAnilist(result, id)
                    } else result
                    publishLoadedMeta(
                        requestKey = requestKey,
                        meta = finalMeta,
                        fallbackItemId = metaLookupId,
                        fallbackItemType = effectiveType,
                        mdbListSettings = mdbListSettings,
                        metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
                    )
                    return@launch
                }
            }

            val tmdbMeta = tryFetchTmdbFallbackMeta(type = effectiveType, id = metaLookupId)
            if (tmdbMeta != null) {
                val finalMeta = if (isAnilistItem) {
                    com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.adaptCinemetaForAnilist(tmdbMeta, id)
                } else tmdbMeta
                publishLoadedMeta(
                    requestKey = requestKey,
                    meta = finalMeta,
                    fallbackItemId = metaLookupId,
                    fallbackItemType = effectiveType,
                    mdbListSettings = mdbListSettings,
                    metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
                )
                return@launch
            }

            log.w { "No addon provides meta for type=$type id=$id (lookupId=$metaLookupId)" }
            _uiState.value = MetaDetailsUiState(
                errorMessage = getString(Res.string.details_no_addon_meta),
            )
            activeRequestKey = null
        }
    }

    fun peek(type: String, id: String): MetaDetails? {
        val requestKey = "$type:$id"
        val currentMeta = _uiState.value.meta?.takeIf { it.type == type && it.id == id }
        if (currentMeta != null) return currentMeta

        val metaScreenSettingsFingerprint = buildMetaScreenSettingsFingerprint(MdbListSettingsRepository.snapshot())
        val cachedEntry = cachedMetaByRequestKey[requestKey] ?: return null
        return cachedEntry.metaScreenMeta
            ?.takeIf { cachedEntry.metaScreenSettingsFingerprint == metaScreenSettingsFingerprint }
            ?: cachedEntry.baseMeta
    }

    fun clear() {
        activeRequestKey = null
        cachedMetaByRequestKey.clear()
        _uiState.value = MetaDetailsUiState()
    }

    suspend fun fetch(type: String, id: String, cacheResult: Boolean = true): MetaDetails? {
        val requestKey = "$type:$id"
        cachedMetaByRequestKey[requestKey]?.let { return it.baseMeta }

        val isAnilistItem = id.startsWith("ani_", ignoreCase = true) || id.startsWith("anilist:", ignoreCase = true)
        val effectiveType = if (type == "movie") "movie" else "series"

        val anilistId = if (isAnilistItem) {
            com.nuvio.app.features.anilist.AnilistTrackerCoordinator.extractAnilistId(id)
        } else {
            when {
                id.startsWith("tt", ignoreCase = true) -> com.nuvio.app.features.anilist.AnilistApi.resolveArmAnilistId("imdb", id)
                id.startsWith("tmdb:", ignoreCase = true) -> com.nuvio.app.features.anilist.AnilistApi.resolveArmAnilistId("themoviedb", id.removePrefix("tmdb:"))
                id.all(Char::isDigit) -> com.nuvio.app.features.anilist.AnilistApi.resolveArmAnilistId("themoviedb", id)
                else -> null
            }
        }

        if (anilistId != null) {
            val anilistMeta = com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.resolveMetaDetails("ani_$anilistId")
            if (anilistMeta != null) {
                if (cacheResult) {
                    cachedMetaByRequestKey[requestKey] = CachedMetaEntry(baseMeta = anilistMeta)
                }
                return anilistMeta
            }
        }

        val metaLookupId = resolveMetaLookupId(itemId = id, itemType = type)
        val manifests = findReadyMetaManifests(type = effectiveType, id = metaLookupId)

        for (manifest in manifests) {
            val result = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                tryFetchMeta(manifest, effectiveType, metaLookupId, includeMdbList = false)
            }
            if (result != null) {
                val finalMeta = if (isAnilistItem) {
                    com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.adaptCinemetaForAnilist(result, id)
                } else result
                if (cacheResult) {
                    cachedMetaByRequestKey[requestKey] = CachedMetaEntry(baseMeta = finalMeta)
                }
                return finalMeta
            }
        }

        val tmdbMeta = tryFetchTmdbFallbackMeta(type = effectiveType, id = metaLookupId)
        if (tmdbMeta != null) {
            val finalMeta = if (isAnilistItem) {
                com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.adaptCinemetaForAnilist(tmdbMeta, id)
            } else tmdbMeta
            if (cacheResult) {
                cachedMetaByRequestKey[requestKey] = CachedMetaEntry(baseMeta = finalMeta)
            }
            return finalMeta
        }

        return null
    }

    private const val FETCH_TIMEOUT_MS = 5_000L
    private const val METADATA_PROVIDER_READY_TIMEOUT_MS = 10_000L
    private const val TMDB_ENRICH_TIMEOUT_MS = 5_000L
    private const val MDBLIST_ENRICH_TIMEOUT_MS = 5_000L

    private suspend fun tryFetchMeta(
        manifest: AddonManifest,
        type: String,
        id: String,
        includeMdbList: Boolean,
        enrichWithTmdb: Boolean = true,
    ): MetaDetails? {
        val url = buildAddonResourceUrl(
            manifestUrl = manifest.transportUrl,
            resource = "meta",
            type = type,
            id = id,
        )

        return try {
            TmdbSettingsRepository.ensureLoaded()
            log.d { "Fetching meta from: $url" }
            val payload = fetchAddonResponseText(url)
            log.d { "Raw payload length=${payload.length}, first 500 chars: ${payload.take(500)}" }
            val result = MetaDetailsParser.parse(payload)
            if (!enrichWithTmdb) {
                return result
            }
            val tmdbEnriched = withTimeoutOrNull(TMDB_ENRICH_TIMEOUT_MS) {
                TmdbMetadataService.enrichMeta(
                    meta = result,
                    fallbackItemId = id,
                    settings = TmdbSettingsRepository.snapshot(),
                )
            } ?: result
            val enriched = if (includeMdbList) {
                MdbListSettingsRepository.ensureLoaded()
                withTimeoutOrNull(MDBLIST_ENRICH_TIMEOUT_MS) {
                    MdbListMetadataService.enrichMeta(
                        meta = tmdbEnriched,
                        fallbackItemId = id,
                        settings = MdbListSettingsRepository.snapshot(),
                    )
                } ?: tmdbEnriched
            } else {
                tmdbEnriched
            }
            enriched
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            log.e(e) { "Failed to fetch/parse meta from $url (manifest=${manifest.transportUrl})" }
            null
        }
    }

    private val cinemetaDefaultManifest = AddonManifest(
        id = "org.stremio.cinemeta",
        name = "Cinemeta",
        version = "3.0.0",
        description = "Cinemeta Official Metadata",
        resources = listOf(
            AddonResource(
                name = "meta",
                types = listOf("movie", "series"),
                idPrefixes = listOf("tt"),
            )
        ),
        types = listOf("movie", "series"),
        catalogs = emptyList(),
        transportUrl = "https://v3-cinemeta.strem.io/manifest.json",
    )

    private suspend fun findReadyMetaManifests(type: String, id: String): List<AddonManifest> {
        AddonRepository.initialize()

        val active = findMetaManifests(AddonRepository.uiState.value, type, id)
        if (active.isNotEmpty()) return active

        if (id.startsWith("tt", ignoreCase = true)) {
            return listOf(cinemetaDefaultManifest)
        }

        if (!AddonRepository.uiState.value.hasPendingEnabledAddonManifests()) {
            return emptyList()
        }

        val readyState = withTimeoutOrNull(METADATA_PROVIDER_READY_TIMEOUT_MS) {
            AddonRepository.uiState.first { state ->
                findMetaManifests(state, type, id).isNotEmpty() ||
                    !state.hasPendingEnabledAddonManifests()
            }
        } ?: AddonRepository.uiState.value

        val resolved = findMetaManifests(readyState, type, id)
        if (resolved.isEmpty() && id.startsWith("tt", ignoreCase = true)) {
            return listOf(cinemetaDefaultManifest)
        }
        return resolved
    }

    private fun findMetaManifests(state: com.nuvio.app.features.addons.AddonsUiState, type: String, id: String): List<AddonManifest> =
        state.addons
            .enabledAddons()
            .mapNotNull { it.manifest }
            .filter { manifest ->
                manifest.resources.any { resource ->
                    resource.name == "meta" &&
                        resource.types.contains(type) &&
                        (resource.idPrefixes.isEmpty() || resource.idPrefixes.any { id.startsWith(it) })
                }
            }

    private fun com.nuvio.app.features.addons.AddonsUiState.hasPendingEnabledAddonManifests(): Boolean =
        addons.enabledAddons().any { addon -> addon.manifest == null && addon.isRefreshing }

    private suspend fun resolveMetaLookupId(itemId: String, itemType: String): String {
        val match = Regex("tt\\d+").find(itemId)?.value
        if (match != null) return match

        if (itemId.startsWith("ani_", ignoreCase = true) || itemId.startsWith("anilist:", ignoreCase = true)) {
            val anilistId = com.nuvio.app.features.anilist.AnilistTrackerCoordinator.extractAnilistId(itemId)
            if (anilistId != null) {
                val arm = com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.resolveArmMapping(anilistId)
                val resolved = arm.imdbId ?: arm.tmdbId?.let { "tmdb:$it" } ?: arm.kitsuId
                if (!resolved.isNullOrBlank()) {
                    return resolved
                }
            }
        }

        return itemId
    }

    private suspend fun tryFetchTmdbFallbackMeta(type: String, id: String): MetaDetails? =
        withTimeoutOrNull(TMDB_ENRICH_TIMEOUT_MS) {
            TmdbMetadataService.fetchStandaloneMeta(
                type = type,
                id = id,
                settings = TmdbSettingsRepository.snapshot(),
            )
        }

    private suspend fun publishLoadedMeta(
        requestKey: String,
        meta: MetaDetails,
        fallbackItemId: String,
        fallbackItemType: String,
        mdbListSettings: com.nuvio.app.features.mdblist.MdbListSettings,
        metaScreenSettingsFingerprint: String,
    ) {
        val cachedEntry = CachedMetaEntry(baseMeta = meta)
        cachedMetaByRequestKey[requestKey] = cachedEntry

        // 1. Immediately render base meta on screen with 0ms perceived delay
        _uiState.value = MetaDetailsUiState(
            isLoading = false,
            meta = meta.withUnreleasedFilter(),
        )

        if (!shouldEnrichForMetaScreen(meta, fallbackItemId, mdbListSettings)) {
            activeRequestKey = requestKey
            return
        }

        // 2. Asynchronously enrich in background with parallel coroutines
        val enrichedMeta = withContext(Dispatchers.Default) {
            enrichForMetaScreen(
                requestKey = requestKey,
                meta = meta,
                fallbackItemId = fallbackItemId,
                fallbackItemType = fallbackItemType,
                settings = mdbListSettings,
                settingsFingerprint = metaScreenSettingsFingerprint,
            )
        }
        cachedMetaByRequestKey[requestKey] = cachedEntry.copy(
            metaScreenMeta = enrichedMeta,
            metaScreenSettingsFingerprint = metaScreenSettingsFingerprint,
        )
        _uiState.value = MetaDetailsUiState(meta = enrichedMeta.withUnreleasedFilter(), isLoading = false)
        activeRequestKey = requestKey
    }

    private suspend fun enrichForMetaScreen(
        requestKey: String,
        meta: MetaDetails,
        fallbackItemId: String,
        fallbackItemType: String,
        settings: com.nuvio.app.features.mdblist.MdbListSettings,
        settingsFingerprint: String,
    ): MetaDetails = coroutineScope {
        val tmdbSettings = TmdbSettingsRepository.snapshot()
        val isAnilist = meta.id.startsWith("ani_", ignoreCase = true) || meta.id.startsWith("anilist:", ignoreCase = true)

        var currentMeta = meta
        val mutex = Mutex()

        suspend fun emitUpdate(transform: (MetaDetails) -> MetaDetails) {
            val updated = mutex.withLock {
                currentMeta = transform(currentMeta)
                currentMeta
            }
            if (activeRequestKey == requestKey) {
                _uiState.value = MetaDetailsUiState(meta = updated.withUnreleasedFilter(), isLoading = false)
            }
        }

        val tmdbJob = launch {
            if (tmdbSettings.enabled && tmdbSettings.hasApiKey) {
                val tmdbEnriched = runCatching {
                    withTimeoutOrNull(TMDB_ENRICH_TIMEOUT_MS) {
                        TmdbMetadataService.enrichMeta(
                            meta = meta,
                            fallbackItemId = fallbackItemId,
                            settings = tmdbSettings,
                        )
                    }
                }.getOrNull()

                if (tmdbEnriched != null) {
                    emitUpdate { current ->
                        if (isAnilist) {
                            current.copy(
                                videos = current.videos.mapIndexed { idx, currentVid ->
                                    val enrichedVid = tmdbEnriched.videos.firstOrNull { ev ->
                                        (ev.season == currentVid.season && ev.episode == currentVid.episode) ||
                                            (ev.episode == currentVid.episode)
                                    } ?: tmdbEnriched.videos.getOrNull(idx)
                                    if (enrichedVid != null) {
                                        currentVid.copy(
                                            title = if (tmdbSettings.useEpisodes) enrichedVid.title ?: currentVid.title else currentVid.title,
                                            overview = if (tmdbSettings.useEpisodes) enrichedVid.overview ?: currentVid.overview else currentVid.overview,
                                            released = if (tmdbSettings.useReleaseDates) enrichedVid.released ?: currentVid.released else currentVid.released,
                                            thumbnail = if (tmdbSettings.useEpisodes) enrichedVid.thumbnail ?: currentVid.thumbnail else currentVid.thumbnail,
                                            runtime = if (tmdbSettings.useEpisodes) enrichedVid.runtime ?: currentVid.runtime else currentVid.runtime,
                                        )
                                    } else currentVid
                                },
                            )
                        } else {
                            tmdbEnriched.copy(
                                id = current.id,
                                type = current.type,
                                name = tmdbEnriched.name,
                                genres = if (current.genres.isNotEmpty()) current.genres else tmdbEnriched.genres,
                                logo = current.logo ?: tmdbEnriched.logo ?: meta.logo,
                                background = tmdbEnriched.background ?: current.background ?: meta.background,
                                poster = current.poster ?: tmdbEnriched.poster ?: meta.poster,
                                videos = tmdbEnriched.videos.mapIndexed { idx, enrichedVid ->
                                    val currentVid = current.videos.getOrNull(idx)
                                    if (!currentVid?.seasonPoster.isNullOrBlank()) {
                                        enrichedVid.copy(seasonPoster = currentVid.seasonPoster)
                                    } else enrichedVid
                                },
                                cast = tmdbEnriched.cast,
                                productionCompanies = tmdbEnriched.productionCompanies,
                                networks = tmdbEnriched.networks,
                                trailers = if (tmdbEnriched.trailers.isNotEmpty()) tmdbEnriched.trailers else current.trailers,
                                moreLikeThis = tmdbEnriched.moreLikeThis,
                                moreLikeThisSource = tmdbEnriched.moreLikeThisSource,
                                description = tmdbEnriched.description,
                                releaseInfo = meta.releaseInfo ?: tmdbEnriched.releaseInfo,
                                status = tmdbEnriched.status,
                                lastAirDate = tmdbEnriched.lastAirDate,
                                externalRatings = current.externalRatings.ifEmpty { tmdbEnriched.externalRatings },
                            )
                        }
                    }
                }
            }
        }

        // 3. MDBList job: External ratings (IMDb, TMDb, Trakt, RT, AniList) - Streams in independently!
        val mdbListJob = launch {
            if (isAnilist) {
                // For anime, fetch genuine MAL score and certification from MAL/Jikan if not already present
                val anilistId = meta.id.removePrefix("ani_").toIntOrNull()
                val idMal = anilistId?.let { com.nuvio.app.features.anilist.AnilistApi.fetchMediaById(it)?.idMal }
                val malMeta = idMal?.let { com.nuvio.app.features.anilist.AnilistApi.fetchMalMetadata(it) }
                val realMalScore = malMeta?.score
                val ageRating = malMeta?.ageRating

                if (realMalScore != null && realMalScore > 0) {
                    emitUpdate { current ->
                        val existingWithoutMal = current.externalRatings.filterNot { it.source == "mal" }
                        val updated = existingWithoutMal + MetaExternalRating(source = "mal", value = realMalScore)
                        current.copy(
                            externalRatings = updated,
                            ageRating = current.ageRating ?: ageRating,
                        )
                    }
                }
            } else {
                val mdbListRatings = runCatching {
                    withTimeoutOrNull(MDBLIST_ENRICH_TIMEOUT_MS) {
                        MdbListMetadataService.enrichMeta(
                            meta = meta,
                            fallbackItemId = fallbackItemId,
                            settings = settings,
                        )
                    }?.externalRatings.orEmpty()
                }.getOrNull().orEmpty()

                if (mdbListRatings.isNotEmpty()) {
                    emitUpdate { current ->
                        current.copy(externalRatings = mdbListRatings)
                    }
                }
            }
        }

        // 4. Trakt job: Related titles / recommendations (Only for standard non-anime movies/shows)
        val traktJob = launch {
            if (!isAnilist) {
                val traktMeta = runCatching {
                    applyMoreLikeThisSource(
                        meta = meta,
                        fallbackItemId = fallbackItemId,
                        fallbackItemType = fallbackItemType,
                    )
                }.getOrNull()

                if (traktMeta != null && traktMeta.moreLikeThis.isNotEmpty()) {
                    emitUpdate { current ->
                        current.copy(
                            moreLikeThis = traktMeta.moreLikeThis,
                            moreLikeThisSource = traktMeta.moreLikeThisSource,
                        )
                    }
                }
            }
        }

        tmdbJob.join()
        mdbListJob.join()
        traktJob.join()

        mutex.withLock { currentMeta }
    }

    private suspend fun applyMoreLikeThisSource(
        meta: MetaDetails,
        fallbackItemId: String,
        fallbackItemType: String,
    ): MetaDetails {
        val isAnilist = meta.id.startsWith("ani_", ignoreCase = true) || meta.id.startsWith("anilist:", ignoreCase = true)
        if (isAnilist && meta.moreLikeThis.isNotEmpty()) {
            return meta
        }

        TrackingSettingsRepository.ensureLoaded()
        TraktAuthRepository.ensureLoaded()
        TmdbSettingsRepository.ensureLoaded()

        val trackingSettings = TrackingSettingsRepository.uiState.value
        val isTraktAuthenticated = TraktAuthRepository.uiState.value.mode == TraktConnectionMode.CONNECTED
        val shouldUseTrakt = shouldUseTraktMoreLikeThis(
            isAuthenticated = isTraktAuthenticated,
            source = trackingSettings.moreLikeThisSource,
        ) && supportsMoreLikeThis(meta, fallbackItemType)

        if (shouldUseTrakt) {
            val items = runCatching {
                TraktRelatedRepository.getRelated(
                    meta = meta,
                    fallbackItemId = fallbackItemId,
                    fallbackItemType = fallbackItemType,
                )
            }.onFailure { error ->
                log.w { "Failed to load Trakt related titles for ${meta.id}: ${error.message}" }
            }.getOrDefault(emptyList())

            return meta.copy(
                moreLikeThis = items,
                moreLikeThisSource = MoreLikeThisSource.TRAKT.takeIf { items.isNotEmpty() },
            )
        }

        val tmdbSettings = TmdbSettingsRepository.snapshot()
        if (!tmdbSettings.enabled || !tmdbSettings.useMoreLikeThis) {
            return meta.copy(moreLikeThis = emptyList(), moreLikeThisSource = null)
        }

        return meta.copy(
            moreLikeThisSource = MoreLikeThisSource.TMDB.takeIf { meta.moreLikeThis.isNotEmpty() },
        )
    }

    private fun shouldFetchMdbListOnMetaScreen(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: com.nuvio.app.features.mdblist.MdbListSettings,
    ): Boolean = MdbListMetadataService.shouldFetchForMeta(
        meta = meta,
        fallbackItemId = fallbackItemId,
        settings = settings,
    )

    private fun shouldEnrichForMetaScreen(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: com.nuvio.app.features.mdblist.MdbListSettings,
    ): Boolean {
        val tmdbSettings = TmdbSettingsRepository.snapshot()
        if (tmdbSettings.enabled && tmdbSettings.hasApiKey) return true
        if (shouldFetchMdbListOnMetaScreen(meta, fallbackItemId, settings)) return true
        return shouldApplyMoreLikeThisSource(meta)
    }

    private fun shouldApplyMoreLikeThisSource(meta: MetaDetails): Boolean {
        TrackingSettingsRepository.ensureLoaded()
        TraktAuthRepository.ensureLoaded()
        TmdbSettingsRepository.ensureLoaded()

        val trackingSettings = TrackingSettingsRepository.uiState.value
        val isTraktAuthenticated = TraktAuthRepository.uiState.value.mode == TraktConnectionMode.CONNECTED
        val tmdbSettings = TmdbSettingsRepository.snapshot()
        return shouldUseTraktMoreLikeThis(
            isAuthenticated = isTraktAuthenticated,
            source = trackingSettings.moreLikeThisSource,
        ) || !tmdbSettings.enabled || !tmdbSettings.useMoreLikeThis || meta.moreLikeThisSource == null && meta.moreLikeThis.isNotEmpty()
    }

    private fun buildMetaScreenSettingsFingerprint(
        settings: com.nuvio.app.features.mdblist.MdbListSettings,
    ): String {
        TrackingSettingsRepository.ensureLoaded()
        TraktAuthRepository.ensureLoaded()
        TmdbSettingsRepository.ensureLoaded()
        val providers = settings.enabledProvidersInPriorityOrder().joinToString(",")
        val trackingSettings = TrackingSettingsRepository.uiState.value
        val traktAuthMode = TraktAuthRepository.uiState.value.mode
        val tmdbSettings = TmdbSettingsRepository.snapshot()
        return buildString {
            append("${settings.enabled}:${settings.apiKey.trim()}:$providers")
            append("|more_like=${trackingSettings.moreLikeThisSource}:$traktAuthMode")
            append("|tmdb=${tmdbSettings.enabled}:${tmdbSettings.useMoreLikeThis}:${tmdbSettings.hasApiKey}:${tmdbSettings.language}")
        }
    }

    private fun supportsMoreLikeThis(meta: MetaDetails, fallbackItemType: String): Boolean =
        normalizeMoreLikeThisType(meta.type) != null || normalizeMoreLikeThisType(fallbackItemType) != null

    private fun normalizeMoreLikeThisType(value: String?): String? =
        when (value?.trim()?.lowercase()) {
            "movie", "film" -> "movie"
            "series", "show", "tv", "tvshow" -> "series"
            else -> null
        }

    private fun MetaDetails.withUnreleasedFilter(): MetaDetails {
        if (!HomeCatalogSettingsRepository.snapshot().hideUnreleasedContent) return this
        val todayIsoDate = CurrentDateProvider.todayIsoDate()
        val releasedMoreLikeThis = moreLikeThis.filterReleasedItems(todayIsoDate)
        return copy(
            moreLikeThis = releasedMoreLikeThis,
            moreLikeThisSource = moreLikeThisSource.takeIf { releasedMoreLikeThis.isNotEmpty() },
            collectionItems = collectionItems.filterReleasedItems(todayIsoDate),
        )
    }

   
    fun findEmbeddedStreams(videoId: String): List<com.nuvio.app.features.streams.StreamItem> {
        val meta = _uiState.value.meta ?: return emptyList()
        val videosWithStreams = meta.videos.filter { it.streams.isNotEmpty() }
        if (videosWithStreams.isEmpty()) return emptyList()

        val directMatch = videosWithStreams.firstOrNull { it.id == videoId }
        if (directMatch != null) return directMatch.streams

        val parts = videoId.split(":")
        if (parts.size >= 3) {
            val season = parts[parts.size - 2].toIntOrNull()
            val episode = parts[parts.size - 1].toIntOrNull()
            if (season != null && episode != null) {
                val episodeMatch = videosWithStreams.firstOrNull { it.season == season && it.episode == episode }
                if (episodeMatch != null) return episodeMatch.streams
            }
        }

        val prefixMatch = videosWithStreams.firstOrNull { it.id.startsWith("$videoId:") }
        if (prefixMatch != null) return prefixMatch.streams

        if (videoId == meta.id && videosWithStreams.size == 1) {
            return videosWithStreams.first().streams
        }

        if (videoId == meta.id && videosWithStreams.isNotEmpty()) {
            return videosWithStreams.flatMap { it.streams }
        }

        return emptyList()
    }
}
