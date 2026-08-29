package com.nuvio.app.features.artwork

import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MetaHubArtwork {
    private val imdbRegex = Regex("tt\\d+")
    private val resolvedImdbCache = mutableMapOf<String, String>()

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
                val arm = AnilistMetaDetailsResolver.resolveArmMapping(anilistId)
                val imdb = arm.imdbId
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
}
