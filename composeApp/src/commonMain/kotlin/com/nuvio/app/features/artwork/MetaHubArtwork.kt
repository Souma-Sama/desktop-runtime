package com.nuvio.app.features.artwork

import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MetaHubArtwork {
    private val imdbRegex = Regex("tt\\d+")
    private val resolvedImdbCache = mutableMapOf<String, String>()
    private val malScoreCache = mutableMapOf<String, Double>()

    fun extractImdbId(rawId: String?): String? {
        if (rawId.isNullOrBlank()) return null
        imdbRegex.find(rawId)?.value?.let { return it }
        return resolvedImdbCache[rawId]
    }

    suspend fun resolveImdbId(rawId: String?): String? = withContext(Dispatchers.Default) {
        if (rawId.isNullOrBlank()) return@withContext null
        extractImdbId(rawId)?.let { return@withContext it }

        if (rawId.startsWith("ani_", ignoreCase = true) || rawId.startsWith("anilist:", ignoreCase = true)) {
            val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId)
            if (anilistId != null) {
                val media = AnilistApi.getCachedMedia(anilistId)
                val arm = AnilistMetaDetailsResolver.resolveArmMapping(anilistId)
                val imdb = AnilistMetaDetailsResolver.resolveEffectiveImdbId(media, arm.imdbId)
                if (!imdb.isNullOrBlank()) {
                    resolvedImdbCache[rawId] = imdb
                    return@withContext imdb
                }
            }
        }
        null
    }

    fun getLogoUrl(rawId: String?): String? {
        val imdbId = extractImdbId(rawId) ?: return null
        return "https://images.metahub.space/logo/medium/$imdbId/img"
    }

    fun getBackdropUrl(rawId: String?): String? {
        val imdbId = extractImdbId(rawId) ?: return null
        return "https://images.metahub.space/background/medium/$imdbId/img"
    }

    fun getPosterUrl(rawId: String?): String? {
        val imdbId = extractImdbId(rawId) ?: return null
        return "https://images.metahub.space/poster/medium/$imdbId/img"
    }

    fun getEpisodeStillUrl(rawId: String?, season: Int, episode: Int): String? {
        val imdbId = extractImdbId(rawId) ?: return null
        return "https://episodes.metahub.space/$imdbId/$season/$episode/w780.jpg"
    }

    suspend fun resolveLogoUrl(rawId: String?): String? {
        val imdbId = resolveImdbId(rawId) ?: return null
        return "https://images.metahub.space/logo/medium/$imdbId/img"
    }

    suspend fun resolveBackdropUrl(rawId: String?): String? {
        val imdbId = resolveImdbId(rawId) ?: return null
        return "https://images.metahub.space/background/medium/$imdbId/img"
    }

    fun getMalScore(rawId: String?): Double? {
        if (rawId.isNullOrBlank()) return null
        return malScoreCache[rawId]
    }

    suspend fun resolveMalScore(rawId: String?): Double? = withContext(Dispatchers.Default) {
        if (rawId.isNullOrBlank()) return@withContext null
        malScoreCache[rawId]?.let { return@withContext it }

        val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId)
        if (anilistId != null) {
            val cachedMedia = AnilistApi.getCachedMedia(anilistId)
            val malId = cachedMedia?.idMal ?: AnilistMetaDetailsResolver.resolveArmMapping(anilistId).malId
            if (malId != null && malId > 0) {
                val score = AnilistApi.fetchMalScore(malId)
                if (score != null && score > 0) {
                    malScoreCache[rawId] = score
                    return@withContext score
                }
            }
        }
        null
    }
}
