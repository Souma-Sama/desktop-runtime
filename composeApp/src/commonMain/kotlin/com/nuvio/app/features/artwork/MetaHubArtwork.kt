package com.nuvio.app.features.artwork

object MetaHubArtwork {
    private val imdbRegex = Regex("tt\\d+")

    fun extractImdbId(rawId: String?): String? {
        if (rawId.isNullOrBlank()) return null
        return imdbRegex.find(rawId)?.value
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
}
