package com.nuvio.app.features.fanart

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.details.MetaDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object FanartService {
    private val log = Logger.withTag("FanartService")
    private val json = Json { ignoreUnknownKeys = true }
    private const val BASE_URL = "https://webservice.fanart.tv/v3"

    private val logoCache = mutableMapOf<String, String>()
    private val backdropCache = mutableMapOf<String, String>()
    private val bannerCache = mutableMapOf<String, String>()
    private val posterCache = mutableMapOf<String, String>()
    private val seasonPosterCache = mutableMapOf<String, String>()
    private val negativeCache = mutableSetOf<String>()

    private val imdbRegex = Regex("tt\\d+")

    fun clearCache() {
        logoCache.clear()
        backdropCache.clear()
        bannerCache.clear()
        posterCache.clear()
        seasonPosterCache.clear()
        negativeCache.clear()
    }

    fun getCachedLogo(id: String, type: String): String? {
        val cleanId = extractLookupId(id) ?: return null
        return logoCache["$type:$cleanId"]
    }

    fun getCachedBackdrop(id: String, type: String): String? {
        val cleanId = extractLookupId(id) ?: return null
        return backdropCache["$type:$cleanId"]
    }

    fun getCachedBanner(id: String, type: String): String? {
        val cleanId = extractLookupId(id) ?: return null
        return bannerCache["$type:$cleanId"]
    }

    fun getCachedPoster(id: String, type: String): String? {
        val cleanId = extractLookupId(id) ?: return null
        return posterCache["$type:$cleanId"]
    }

    fun getCachedSeasonPoster(id: String, seasonNumber: Int): String? {
        val cleanId = extractLookupId(id) ?: return null
        return seasonPosterCache["$cleanId:$seasonNumber"]
    }

    suspend fun resolveLogo(id: String, type: String): String? = withContext(Dispatchers.Default) {
        val settings = FanartSettingsRepository.snapshot()
        if (!settings.enabled || !settings.hasApiKey || !settings.useClearLogos) return@withContext null

        val cleanId = resolveLookupId(id, type) ?: return@withContext null
        val cacheKey = "$type:$cleanId"

        logoCache[cacheKey]?.let { return@withContext it }
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
                        }
                    }
                    if (settings.useBanners) {
                        selectBestImage(movie.movieBanner, settings.preferEnglishLogos)?.url?.let {
                            bannerCache[cacheKey] = it
                        }
                    }
                    if (settings.usePosters) {
                        selectBestImage(movie.moviePoster, settings.preferEnglishLogos)?.url?.let {
                            posterCache[cacheKey] = it
                        }
                    }

                    if (logo != null) {
                        logoCache[cacheKey] = logo
                        return@withContext logo
                    }
                }
            } else {
                val tv = fetchTv(cleanId, settings.apiKey)
                if (tv != null) {
                    populateTvCaches(cleanId, tv, settings)
                    val logo = logoCache[cacheKey]
                    if (logo != null) {
                        return@withContext logo
                    }
                }
            }
            negativeCache.add(cacheKey)
            null
        } catch (e: Throwable) {
            log.w(e) { "Failed to resolve Fanart logo for $cacheKey" }
            negativeCache.add(cacheKey)
            null
        }
    }

    suspend fun resolveSeasonPoster(id: String, type: String, seasonNumber: Int): String? = withContext(Dispatchers.Default) {
        val cached = getCachedSeasonPoster(id, seasonNumber)
        if (cached != null) return@withContext cached

        val settings = FanartSettingsRepository.snapshot()
        if (!settings.enabled || !settings.hasApiKey) return@withContext null

        val cleanId = resolveLookupId(id, type) ?: return@withContext null
        val sKey = "$cleanId:$seasonNumber"
        seasonPosterCache[sKey]?.let { return@withContext it }

        try {
            val tv = fetchTv(cleanId, settings.apiKey)
            if (tv != null) {
                populateTvCaches(cleanId, tv, settings)
                return@withContext seasonPosterCache[sKey]
            }
            null
        } catch (e: Throwable) {
            log.w(e) { "Failed to resolve Fanart season poster for $sKey" }
            null
        }
    }

    suspend fun enrichMetaDetails(
        meta: MetaDetails,
        fallbackItemId: String,
        settings: FanartSettings,
    ): MetaDetails = withContext(Dispatchers.Default) {
        if (!settings.enabled || !settings.hasApiKey) return@withContext meta

        val targetId = meta.id.takeIf { it.isNotBlank() } ?: fallbackItemId
        val cleanId = resolveLookupId(targetId, meta.type) ?: return@withContext meta
        val cacheKey = "${meta.type}:$cleanId"

        val isMovie = isMovieType(meta.type)
        try {
            if (isMovie) {
                val movie = fetchMovie(cleanId, settings.apiKey) ?: return@withContext meta
                var updated = meta

                if (settings.useClearLogos) {
                    selectBestImage(movie.hdMovieLogo.ifEmpty { movie.movieLogo }, settings.preferEnglishLogos)?.url?.let {
                        logoCache[cacheKey] = it
                        if (updated.logo.isNullOrBlank() || updated.logo?.contains("fanart") == true) {
                            updated = updated.copy(logo = it)
                        }
                    }
                }
                if (settings.useHeroBackdrops) {
                    selectBestImage(movie.movieBackground, settings.preferEnglishLogos)?.url?.let {
                        backdropCache[cacheKey] = it
                        if (updated.background.isNullOrBlank() || updated.background?.contains("fanart") == true) {
                            updated = updated.copy(background = it)
                        }
                    }
                }
                if (settings.usePosters) {
                    selectBestImage(movie.moviePoster, settings.preferEnglishLogos)?.url?.let {
                        posterCache[cacheKey] = it
                        if (updated.poster.isNullOrBlank() || updated.poster?.contains("fanart") == true) {
                            updated = updated.copy(poster = it)
                        }
                    }
                }
                updated
            } else {
                val tv = fetchTv(cleanId, settings.apiKey) ?: return@withContext meta
                populateTvCaches(cleanId, tv, settings)
                var updated = meta

                if (settings.useClearLogos) {
                    logoCache[cacheKey]?.let {
                        if (updated.logo.isNullOrBlank() || updated.logo?.contains("fanart") == true) {
                            updated = updated.copy(logo = it)
                        }
                    }
                }
                if (settings.useHeroBackdrops) {
                    backdropCache[cacheKey]?.let {
                        if (updated.background.isNullOrBlank() || updated.background?.contains("fanart") == true) {
                            updated = updated.copy(background = it)
                        }
                    }
                }
                if (settings.usePosters) {
                    posterCache[cacheKey]?.let {
                        if (updated.poster.isNullOrBlank() || updated.poster?.contains("fanart") == true) {
                            updated = updated.copy(poster = it)
                        }
                    }
                }

                // Enrich season posters into videos
                val updatedVideos = updated.videos.map { vid ->
                    val s = vid.season ?: 1
                    val fanartSeasonPoster = seasonPosterCache["$cleanId:$s"]
                    if (fanartSeasonPoster != null) {
                        vid.copy(seasonPoster = fanartSeasonPoster)
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

    private fun populateTvCaches(cleanId: String, tv: FanartTvResponse, settings: FanartSettings) {
        val cacheKey = "series:$cleanId"

        val logo = selectBestImage(
            images = tv.hdTvLogo.ifEmpty { tv.clearLogo },
            preferEnglish = settings.preferEnglishLogos,
        )?.url
        if (logo != null) {
            logoCache[cacheKey] = logo
            logoCache["tv:$cleanId"] = logo
        }

        if (settings.useHeroBackdrops) {
            selectBestImage(tv.showBackground, settings.preferEnglishLogos)?.url?.let {
                backdropCache[cacheKey] = it
                backdropCache["tv:$cleanId"] = it
            }
        }
        if (settings.useBanners) {
            selectBestImage(tv.tvBanner, settings.preferEnglishLogos)?.url?.let {
                bannerCache[cacheKey] = it
                bannerCache["tv:$cleanId"] = it
            }
        }
        if (settings.usePosters) {
            selectBestImage(tv.tvPoster, settings.preferEnglishLogos)?.url?.let {
                posterCache[cacheKey] = it
                posterCache["tv:$cleanId"] = it
            }
        }

        // Cache season posters
        val groupedBySeason = tv.seasonPoster.groupBy { it.season?.toIntOrNull() ?: 1 }
        groupedBySeason.forEach { (seasonNum, images) ->
            val bestSeasonPoster = selectBestImage(images, settings.preferEnglishLogos)?.url
            if (bestSeasonPoster != null) {
                seasonPosterCache["$cleanId:$seasonNum"] = bestSeasonPoster
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
        val url = "$BASE_URL/tv/$id?api_key=$apiKey"
        return try {
            val responseText = httpGetText(url)
            if (responseText.isBlank() || responseText.contains("\"status\":\"error\"")) null
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

        if (raw.startsWith("tmdb:", ignoreCase = true)) {
            val numeric = raw.substringAfter("tmdb:").substringBefore(':').trim()
            if (numeric.all(Char::isDigit)) return numeric
        }

        if (raw.all(Char::isDigit)) return raw

        return null
    }

    suspend fun resolveLookupId(id: String, type: String = "tv"): String? {
        extractLookupId(id)?.let { return it }

        val raw = id.trim()
        if (raw.startsWith("ani_", ignoreCase = true) || raw.startsWith("anilist:", ignoreCase = true)) {
            val anilistId = com.nuvio.app.features.anilist.AnilistTrackerCoordinator.extractAnilistId(raw)
            if (anilistId != null) {
                val arm = com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.resolveArmMapping(anilistId)
                if (!arm.imdbId.isNullOrBlank()) return arm.imdbId
                if (arm.tmdbId != null && arm.tmdbId > 0) return arm.tmdbId.toString()
            }
        }

        return null
    }

    private fun isMovieType(type: String): Boolean {
        val normalized = type.trim().lowercase()
        return normalized == "movie" || normalized == "film"
    }
}
