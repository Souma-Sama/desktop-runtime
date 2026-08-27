package com.nuvio.app.features.fanart

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.details.MetaDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object FanartService {
    private val log = Logger.withTag("FanartService")
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE_URL = "https://webservice.fanart.tv/v3"

    private val logoCache = mutableMapOf<String, String>()
    private val backdropCache = mutableMapOf<String, String>()
    private val bannerCache = mutableMapOf<String, String>()
    private val posterCache = mutableMapOf<String, String>()
    private val seasonPosterCache = mutableMapOf<String, String>()
    private val lookupIdCache = mutableMapOf<String, String>()
    private val negativeCache = mutableSetOf<String>()

    private val imdbRegex = Regex("tt\\d+")

    fun formatBetterPoster(imdbId: String, template: String? = null): String? {
        if (!imdbId.startsWith("tt")) return null
        val effectiveTemplate = template?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            if (effectiveTemplate.contains("%s")) {
                effectiveTemplate.replace("%s", imdbId)
            } else if (effectiveTemplate.contains("{id}")) {
                effectiveTemplate.replace("{id}", imdbId)
            } else if (effectiveTemplate.contains("{imdb}")) {
                effectiveTemplate.replace("{imdb}", imdbId)
            } else {
                effectiveTemplate
            }
        }.getOrNull()
    }

    fun clearCache() {
        logoCache.clear()
        backdropCache.clear()
        bannerCache.clear()
        posterCache.clear()
        seasonPosterCache.clear()
        lookupIdCache.clear()
        negativeCache.clear()
    }

    fun getCachedLogo(id: String, type: String = "tv"): String? {
        val direct = logoCache["$type:$id"]
        if (direct != null) return direct
        val cleanId = extractLookupId(id) ?: return null
        return logoCache["$type:$cleanId"]
    }

    fun getCachedBackdrop(id: String, type: String = "tv"): String? {
        val direct = backdropCache["$type:$id"]
        if (direct != null) return direct
        val cleanId = extractLookupId(id) ?: return null
        return backdropCache["$type:$cleanId"]
    }

    fun getCachedBanner(id: String, type: String = "tv"): String? {
        val direct = bannerCache["$type:$id"]
        if (direct != null) return direct
        val cleanId = extractLookupId(id) ?: return null
        return bannerCache["$type:$cleanId"]
    }

    fun getCachedPoster(id: String, type: String = "tv"): String? {
        val direct = posterCache["$type:$id"]
        if (direct != null) return direct
        val cleanId = extractLookupId(id) ?: return null
        return posterCache["$type:$cleanId"]
    }

    fun getCachedSeasonPoster(id: String, seasonNumber: Int): String? {
        val direct = seasonPosterCache["$id:$seasonNumber"]
        if (direct != null) return direct
        val cleanId = extractLookupId(id) ?: return null
        return seasonPosterCache["$cleanId:$seasonNumber"]
    }

    suspend fun resolveLogo(id: String, type: String): String? = withContext(Dispatchers.Default) {
        val settings = FanartSettingsRepository.snapshot()
        val cleanId = resolveLookupId(id, type)

        if (!settings.enabled || !settings.hasApiKey || !settings.useClearLogos) {
            if (cleanId != null && cleanId.startsWith("tt")) {
                val metahubLogo = "https://images.metahub.space/logo/medium/$cleanId/img"
                val metahubBackdrop = "https://images.metahub.space/background/medium/$cleanId/img"
                logoCache["$type:$id"] = metahubLogo
                backdropCache["$type:$id"] = metahubBackdrop
                return@withContext metahubLogo
            }
            return@withContext null
        }

        if (cleanId == null) return@withContext null
        val cacheKey = "$type:$cleanId"

        logoCache[cacheKey]?.let {
            logoCache["$type:$id"] = it
            return@withContext it
        }
        if (negativeCache.contains(cacheKey)) return@withContext null

        val isMovie = isMovieType(type)
        try {
            if (isMovie) {
                val movie = fetchMovie(cleanId, settings.apiKey)
                if (movie != null) {
                    val logo = selectBestImage(
                        images = movie.hdMovieLogo.ifEmpty { movie.movieLogo },
                        preferEnglish = settings.preferEnglishLogos,
                    )?.url

                    if (settings.useHeroBackdrops) {
                        selectBestImage(movie.movieBackground, settings.preferEnglishLogos)?.url?.let {
                            backdropCache[cacheKey] = it
                            backdropCache["$type:$id"] = it
                        }
                    }
                    if (settings.useBanners) {
                        selectBestImage(movie.movieBanner, settings.preferEnglishLogos)?.url?.let {
                            bannerCache[cacheKey] = it
                            bannerCache["$type:$id"] = it
                        }
                    }
                    if (settings.usePosters) {
                        selectBestImage(movie.moviePoster, settings.preferEnglishLogos)?.url?.let {
                            posterCache[cacheKey] = it
                            posterCache["$type:$id"] = it
                        }
                    }

                    if (logo != null) {
                        logoCache[cacheKey] = logo
                        logoCache["$type:$id"] = logo
                        return@withContext logo
                    }
                }
            } else {
                val tv = fetchTv(cleanId, settings.apiKey)
                if (tv != null) {
                    populateTvCaches(cleanId, tv, settings, id)
                    val logo = logoCache[cacheKey]
                    if (logo != null) {
                        logoCache["$type:$id"] = logo
                        return@withContext logo
                    }
                }
            }
            val metahubFallback = if (cleanId.startsWith("tt")) "https://images.metahub.space/logo/medium/$cleanId/img" else null
            val metahubBackdropFallback = if (cleanId.startsWith("tt")) "https://images.metahub.space/background/medium/$cleanId/img" else null
            if (metahubBackdropFallback != null && backdropCache[cacheKey] == null) {
                backdropCache[cacheKey] = metahubBackdropFallback
                backdropCache["$type:$id"] = metahubBackdropFallback
            }
            if (metahubFallback != null) {
                logoCache[cacheKey] = metahubFallback
                logoCache["$type:$id"] = metahubFallback
                return@withContext metahubFallback
            }
            negativeCache.add(cacheKey)
            null
        } catch (e: Throwable) {
            log.w(e) { "Failed to resolve Fanart logo for $cacheKey" }
            val metahubFallback = if (cleanId.startsWith("tt")) "https://images.metahub.space/logo/medium/$cleanId/img" else null
            val metahubBackdropFallback = if (cleanId.startsWith("tt")) "https://images.metahub.space/background/medium/$cleanId/img" else null
            if (metahubBackdropFallback != null && backdropCache[cacheKey] == null) {
                backdropCache[cacheKey] = metahubBackdropFallback
                backdropCache["$type:$id"] = metahubBackdropFallback
            }
            if (metahubFallback != null) {
                logoCache[cacheKey] = metahubFallback
                logoCache["$type:$id"] = metahubFallback
                return@withContext metahubFallback
            }
            negativeCache.add(cacheKey)
            null
        }
    }

    suspend fun resolvePoster(id: String, type: String = "tv"): String? = withContext(Dispatchers.Default) {
        val cached = getCachedPoster(id, type)
        if (cached != null) return@withContext cached

        val settings = FanartSettingsRepository.snapshot()
        val cleanId = resolveLookupId(id, type) ?: return@withContext null
        val cacheKey = "$type:$cleanId"

        // 1. Fanart.tv (Highest priority when configured)
        if (settings.enabled && settings.hasApiKey && settings.usePosters) {
            val isMovie = isMovieType(type)
            try {
                if (isMovie) {
                    val movie = fetchMovie(cleanId, settings.apiKey)
                    if (movie != null) {
                        val moviePoster = selectBestImage(movie.moviePoster, settings.preferEnglishLogos)?.url
                        if (moviePoster != null) {
                            posterCache[cacheKey] = moviePoster
                            posterCache["$type:$id"] = moviePoster
                            return@withContext moviePoster
                        }
                    }
                } else {
                    val tv = fetchTv(cleanId, settings.apiKey)
                    if (tv != null) {
                        populateTvCaches(cleanId, tv, settings, id)
                        val fanartPoster = posterCache[cacheKey]
                            ?: seasonPosterCache["$cleanId:1"]
                            ?: (if (id.isNotBlank()) seasonPosterCache["$id:1"] else null)
                            ?: posterCache["series:$cleanId"]
                            ?: posterCache["tv:$cleanId"]
                        if (fanartPoster != null) {
                            posterCache[cacheKey] = fanartPoster
                            posterCache["$type:$id"] = fanartPoster
                            return@withContext fanartPoster
                        }
                    }
                }
            } catch (e: Throwable) {
                log.w(e) { "Failed to fetch Fanart poster for $cacheKey" }
            }
        }

        // 2. BetterPosters Fallback (Secondary priority: ONLY if Fanart returned nothing or is unconfigured)
        if (settings.useBetterPosters && cleanId.startsWith("tt")) {
            formatBetterPoster(cleanId, settings.betterPostersTemplate)?.let { bp ->
                posterCache[cacheKey] = bp
                posterCache["$type:$id"] = bp
                return@withContext bp
            }
        }

        // 3. AniList Default Cover Poster (Returns null so caller uses AniList media.coverImage)
        null
    }

    suspend fun resolveSeasonPoster(id: String, type: String, seasonNumber: Int): String? = withContext(Dispatchers.Default) {
        val cached = getCachedSeasonPoster(id, seasonNumber)
        if (cached != null) return@withContext cached

        val settings = FanartSettingsRepository.snapshot()
        val cleanId = resolveLookupId(id, type) ?: return@withContext null
        val sKey = "$cleanId:$seasonNumber"
        seasonPosterCache[sKey]?.let {
            seasonPosterCache["$id:$seasonNumber"] = it
            return@withContext it
        }

        if (settings.enabled && settings.hasApiKey) {
            try {
                val tv = fetchTv(cleanId, settings.apiKey)
                if (tv != null) {
                    populateTvCaches(cleanId, tv, settings, id)
                    seasonPosterCache[sKey]?.let { return@withContext it }
                }
            } catch (e: Throwable) {
                log.w(e) { "Failed to resolve Fanart season poster for $sKey" }
            }
        }

        // Fallback: BetterPosters for season 1 or movies
        if (settings.useBetterPosters && seasonNumber <= 1 && cleanId.startsWith("tt")) {
            formatBetterPoster(cleanId, settings.betterPostersTemplate)?.let { bp ->
                seasonPosterCache[sKey] = bp
                seasonPosterCache["$id:$seasonNumber"] = bp
                return@withContext bp
            }
        }

        null
    }

    suspend fun enrichMetaDetails(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: FanartSettings,
    ): MetaDetails = withContext(Dispatchers.Default) {
        val targetId = meta.id.takeIf { it.isNotBlank() } ?: fallbackItemId
        val cleanId = resolveLookupId(targetId, meta.type) ?: return@withContext meta
        val cacheKey = "${meta.type}:$cleanId"

        val isMovie = isMovieType(meta.type)
        try {
            if (isMovie) {
                val movie = if (settings.enabled && settings.hasApiKey) fetchMovie(cleanId, settings.apiKey) else null
                var updated = meta

                if (movie != null) {
                    if (settings.useClearLogos) {
                        selectBestImage(movie.hdMovieLogo.ifEmpty { movie.movieLogo }, settings.preferEnglishLogos)?.url?.let {
                            logoCache[cacheKey] = it
                            logoCache["${meta.type}:$targetId"] = it
                            updated = updated.copy(logo = it)
                        }
                    }
                    if (settings.useHeroBackdrops) {
                        selectBestImage(movie.movieBackground, settings.preferEnglishLogos)?.url?.let {
                            backdropCache[cacheKey] = it
                            backdropCache["${meta.type}:$targetId"] = it
                            updated = updated.copy(background = it)
                        }
                    }
                    if (settings.usePosters) {
                        selectBestImage(movie.moviePoster, settings.preferEnglishLogos)?.url?.let {
                            posterCache[cacheKey] = it
                            posterCache["${meta.type}:$targetId"] = it
                            updated = updated.copy(poster = it)
                        }
                    }
                }

                if (settings.useBetterPosters && (updated.poster.isNullOrBlank() || !settings.usePosters) && cleanId.startsWith("tt")) {
                    formatBetterPoster(cleanId, settings.betterPostersTemplate)?.let { bp ->
                        posterCache[cacheKey] = bp
                        posterCache["${meta.type}:$targetId"] = bp
                        if (updated.poster.isNullOrBlank()) {
                            updated = updated.copy(poster = bp)
                        }
                    }
                }

                updated
            } else {
                val tv = if (settings.enabled && settings.hasApiKey) fetchTv(cleanId, settings.apiKey) else null
                if (tv != null) {
                    populateTvCaches(cleanId, tv, settings, targetId)
                }
                var updated = meta

                if (settings.useClearLogos) {
                    val resolvedLogo = logoCache["${meta.type}:$targetId"] ?: logoCache[cacheKey] ?: logoCache["tv:$cleanId"] ?: logoCache["series:$cleanId"]
                    if (resolvedLogo != null) {
                        updated = updated.copy(logo = resolvedLogo)
                    }
                }
                if (settings.useHeroBackdrops) {
                    val resolvedBg = backdropCache["${meta.type}:$targetId"] ?: backdropCache[cacheKey] ?: backdropCache["tv:$cleanId"] ?: backdropCache["series:$cleanId"]
                    if (resolvedBg != null) {
                        updated = updated.copy(background = resolvedBg)
                    }
                }
                if (settings.usePosters) {
                    val resolvedPoster = posterCache["${meta.type}:$targetId"] ?: posterCache[cacheKey] ?: posterCache["tv:$cleanId"] ?: posterCache["series:$cleanId"]
                    if (resolvedPoster != null) {
                        updated = updated.copy(poster = resolvedPoster)
                    }
                }

                val betterPosterFallback = if (settings.useBetterPosters && cleanId.startsWith("tt")) {
                    formatBetterPoster(cleanId, settings.betterPostersTemplate)?.also { bp ->
                        posterCache[cacheKey] = bp
                        posterCache["${meta.type}:$targetId"] = bp
                    }
                } else null

                // Enrich season posters into videos
                val updatedVideos = updated.videos.map { vid ->
                    val s = vid.season ?: 1
                    val fanartSeasonPoster = seasonPosterCache["$cleanId:$s"] ?: seasonPosterCache["$targetId:$s"]
                    if (fanartSeasonPoster != null) {
                        vid.copy(seasonPoster = fanartSeasonPoster)
                    } else if (s <= 1 && betterPosterFallback != null) {
                        vid.copy(seasonPoster = betterPosterFallback)
                    } else if (vid.seasonPoster.isNullOrBlank() && !updated.poster.isNullOrBlank()) {
                        vid.copy(seasonPoster = updated.poster)
                    } else {
                        vid
                    }
                }
                updated.copy(videos = updatedVideos)
            }
        } catch (e: Throwable) {
            log.w(e) { "Failed to enrich MetaDetails via Fanart for $cleanId" }
            meta
        }
    }

    private fun populateTvCaches(cleanId: String, tv: FanartTvResponse, settings: FanartSettings, rawId: String? = null) {
        val cacheKey = "series:$cleanId"

        val logo = selectBestImage(
            images = tv.hdTvLogo.ifEmpty { tv.clearLogo },
            preferEnglish = settings.preferEnglishLogos,
        )?.url
        if (logo != null) {
            logoCache[cacheKey] = logo
            logoCache["tv:$cleanId"] = logo
            if (rawId != null) {
                logoCache["series:$rawId"] = logo
                logoCache["tv:$rawId"] = logo
            }
        }

        if (settings.useHeroBackdrops) {
            selectBestImage(tv.showBackground, settings.preferEnglishLogos)?.url?.let {
                backdropCache[cacheKey] = it
                backdropCache["tv:$cleanId"] = it
                if (rawId != null) {
                    backdropCache["series:$rawId"] = it
                    backdropCache["tv:$rawId"] = it
                }
            }
        }
        if (settings.useBanners) {
            selectBestImage(tv.tvBanner, settings.preferEnglishLogos)?.url?.let {
                bannerCache[cacheKey] = it
                bannerCache["tv:$cleanId"] = it
                if (rawId != null) {
                    bannerCache["series:$rawId"] = it
                    bannerCache["tv:$rawId"] = it
                }
            }
        }
        // Cache season posters
        val groupedBySeason = tv.seasonPoster.groupBy { it.season?.toIntOrNull() ?: 1 }
        groupedBySeason.forEach { (seasonNum, images) ->
            val bestSeasonPoster = selectBestImage(images, settings.preferEnglishLogos)?.url
            if (bestSeasonPoster != null) {
                seasonPosterCache["$cleanId:$seasonNum"] = bestSeasonPoster
                if (rawId != null) {
                    seasonPosterCache["$rawId:$seasonNum"] = bestSeasonPoster
                }
            }
        }

        if (settings.usePosters) {
            val season1Poster = seasonPosterCache["$cleanId:1"] ?: (if (rawId != null) seasonPosterCache["$rawId:1"] else null)
            val tvPoster = selectBestImage(tv.tvPoster, settings.preferEnglishLogos)?.url
            val bestPoster = season1Poster ?: tvPoster
            if (bestPoster != null) {
                posterCache[cacheKey] = bestPoster
                posterCache["tv:$cleanId"] = bestPoster
                posterCache["series:$cleanId"] = bestPoster
                if (rawId != null) {
                    posterCache["series:$rawId"] = bestPoster
                    posterCache["tv:$rawId"] = bestPoster
                }
            }
        }
    }

    private suspend fun fetchMovie(id: String, apiKey: String): FanartMovieResponse? {
        val url = "$BASE_URL/movies/$id?api_key=$apiKey"
        return try {
            val responseText = httpGetText(url)
            if (responseText.isBlank() || responseText.contains("\"status\":\"error\"")) null
            else json.decodeFromString<FanartMovieResponse>(responseText)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchTv(id: String, apiKey: String): FanartTvResponse? {
        val direct = tryFetchTv(id, apiKey)
        if (direct != null) return direct

        // If id is an IMDb ID and Fanart returned 404 (because Fanart TV uses TVDB IDs), resolve TVDB ID via TMDB
        if (id.startsWith("tt")) {
            val tvdbId = com.nuvio.app.features.tmdb.TmdbService.imdbToTvdbId(id)
            if (!tvdbId.isNullOrBlank() && tvdbId != id) {
                val tvdbResult = tryFetchTv(tvdbId, apiKey)
                if (tvdbResult != null) return tvdbResult
            }
        }
        return null
    }

    private suspend fun tryFetchTv(id: String, apiKey: String): FanartTvResponse? {
        val url = "$BASE_URL/tv/$id?api_key=$apiKey"
        return try {
            val responseText = httpGetText(url)
            if (responseText.isBlank() || responseText.contains("\"status\":\"error\"") || responseText.contains("\"error message\"")) null
            else json.decodeFromString<FanartTvResponse>(responseText)
        } catch (e: Exception) {
            null
        }
    }

    private fun selectBestImage(
        images: List<FanartImage>,
        preferEnglish: Boolean,
    ): FanartImage? {
        if (images.isEmpty()) return null

        val validImages = images.filter { !it.url.isNullOrBlank() }
        if (validImages.isEmpty()) return null

        if (preferEnglish) {
            val englishOrNeutral = validImages.filter { img ->
                val lang = img.lang?.lowercase().orEmpty()
                lang == "en" || lang == "00" || lang.isBlank()
            }
            if (englishOrNeutral.isNotEmpty()) {
                return englishOrNeutral.maxByOrNull { it.likes?.toIntOrNull() ?: 0 } ?: englishOrNeutral.first()
            }
        }

        return validImages.maxByOrNull { it.likes?.toIntOrNull() ?: 0 } ?: validImages.first()
    }

    fun extractLookupId(id: String): String? {
        val raw = id.trim()
        val imdbMatch = imdbRegex.find(raw)?.value
        if (imdbMatch != null) return imdbMatch

        if (raw.contains("tmdb", ignoreCase = true)) {
            val numeric = raw.substringAfterLast(':').substringBefore('/').trim()
            if (numeric.isNotBlank() && numeric.all(Char::isDigit)) return numeric
        }

        if (raw.all(Char::isDigit)) return raw

        return null
    }

    suspend fun resolveLookupId(id: String, type: String = "tv"): String? {
        val raw = id.trim()
        val cached = lookupIdCache[raw]
        if (cached != null) return cached

        val isMovie = isMovieType(type)

        // 1. AniList ID (ani_... or anilist:...)
        if (raw.startsWith("ani_", ignoreCase = true) || raw.startsWith("anilist:", ignoreCase = true)) {
            val anilistId = com.nuvio.app.features.anilist.AnilistTrackerCoordinator.extractAnilistId(raw)
            if (anilistId != null) {
                val arm = com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.resolveArmMapping(anilistId)
                if (isMovie) {
                    val movieId = arm.imdbId ?: arm.tmdbId?.toString()
                    if (!movieId.isNullOrBlank()) {
                        lookupIdCache[raw] = movieId
                        return movieId
                    }
                } else {
                    val tvId = arm.tvdbId ?: arm.imdbId ?: arm.tmdbId?.toString()
                    if (!tvId.isNullOrBlank()) {
                        lookupIdCache[raw] = tvId
                        return tvId
                    }
                }
            }
        }

        val imdbMatch = imdbRegex.find(raw)?.value
        if (imdbMatch != null) {
            lookupIdCache[raw] = imdbMatch
            return imdbMatch
        }

        // 2. Kitsu ID (kitsu:...)
        if (raw.startsWith("kitsu:", ignoreCase = true)) {
            val kId = raw.removePrefix("kitsu:").trim()
            val armUrl = "https://arm.haglund.dev/api/v2/ids?source=kitsu&id=$kId"
            val armText = runCatching { httpGetText(armUrl) }.getOrNull()
            if (!armText.isNullOrBlank()) {
                val obj = runCatching { json.parseToJsonElement(armText) }.getOrNull()
                val imdb = obj?.let { (it as? JsonObject)?.get("imdb")?.let { el -> (el as? JsonPrimitive)?.content } }
                if (!imdb.isNullOrBlank()) {
                    lookupIdCache[raw] = imdb
                    return imdb
                }
            }
        }

        // 3. TMDB prefix or ID
        if (raw.contains("tmdb", ignoreCase = true)) {
            val digits = raw.filter(Char::isDigit)
            if (digits.isNotBlank()) {
                if (isMovie) {
                    lookupIdCache[raw] = digits
                    return digits
                }
                val num = digits.toIntOrNull()
                if (num != null) {
                    val converted = com.nuvio.app.features.tmdb.TmdbService.tmdbToImdb(num, "tv")
                    val result = converted ?: digits
                    lookupIdCache[raw] = result
                    return result
                }
                lookupIdCache[raw] = digits
                return digits
            }
        }

        // 4. Plain numeric ID
        if (raw.all(Char::isDigit)) {
            if (isMovie) {
                lookupIdCache[raw] = raw
                return raw
            }
            val num = raw.toIntOrNull()
            if (num != null) {
                val converted = com.nuvio.app.features.tmdb.TmdbService.tmdbToImdb(num, "tv")
                val result = converted ?: raw
                lookupIdCache[raw] = result
                return result
            }
            lookupIdCache[raw] = raw
            return raw
        }

        return null
    }

    private fun isMovieType(type: String): Boolean {
        val normalized = type.trim().lowercase()
        return normalized == "movie" || normalized == "film"
    }
}
