package com.nuvio.app.features.fanart

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
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
    private val negativeCache = mutableSetOf<String>()

    private val imdbRegex = Regex("tt\\d+")

    fun clearCache() {
        logoCache.clear()
        backdropCache.clear()
        bannerCache.clear()
        posterCache.clear()
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

    suspend fun resolveLogo(id: String, type: String): String? = withContext(Dispatchers.Default) {
        val settings = FanartSettingsRepository.snapshot()
        if (!settings.enabled || !settings.hasApiKey || !settings.useClearLogos) return@withContext null

        val cleanId = extractLookupId(id) ?: return@withContext null
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
                    val logo = selectBestImage(
                        images = tv.hdTvLogo.ifEmpty { tv.clearLogo },
                        preferEnglish = settings.preferEnglishLogos,
                    )?.url

                    if (settings.useHeroBackdrops) {
                        selectBestImage(tv.showBackground, settings.preferEnglishLogos)?.url?.let {
                            backdropCache[cacheKey] = it
                        }
                    }
                    if (settings.useBanners) {
                        selectBestImage(tv.tvBanner, settings.preferEnglishLogos)?.url?.let {
                            bannerCache[cacheKey] = it
                        }
                    }
                    if (settings.usePosters) {
                        selectBestImage(tv.tvPoster, settings.preferEnglishLogos)?.url?.let {
                            posterCache[cacheKey] = it
                        }
                    }

                    if (logo != null) {
                        logoCache[cacheKey] = logo
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

    private fun isMovieType(type: String): Boolean {
        val normalized = type.trim().lowercase()
        return normalized == "movie" || normalized == "film"
    }
}
