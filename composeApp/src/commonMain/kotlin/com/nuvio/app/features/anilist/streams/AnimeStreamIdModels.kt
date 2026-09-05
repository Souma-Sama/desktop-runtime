package com.nuvio.app.features.anilist.streams

import kotlinx.serialization.Serializable

@Serializable
enum class AnimeStreamIdType(val displayName: String, val providerName: String) {
    IMDB("IMDb", "Torrentio, Real-Debrid, KnightCrawler"),
    KITSU("Kitsu", "Anime Kitsu & CyberFlix"),
    TMDB("TMDb", "TMDb & Cinemeta scrapers"),
    ANILIST("AniList", "Native anime providers"),
    CUSTOM("Custom ID", "User defined ID override"),
}

data class AnimeStreamIdOption(
    val type: AnimeStreamIdType,
    val rawId: String,
    val formattedLabel: String,
    val description: String,
    val isRecommended: Boolean = false,
    val season: Int = 1,
)

object AnimeStreamIdFormatter {
    fun formatVideoId(
        option: AnimeStreamIdOption,
        season: Int,
        episode: Int,
        isMovie: Boolean,
        relativeEpisode: Int = episode,
    ): String {
        return when (option.type) {
            AnimeStreamIdType.IMDB -> {
                val base = option.rawId.trim()
                if (isMovie) base else "$base:$season:$episode"
            }
            AnimeStreamIdType.KITSU -> {
                val cleanKitsu = option.rawId.removePrefix("kitsu:").trim()
                if (isMovie) "kitsu:$cleanKitsu" else "kitsu:$cleanKitsu:$relativeEpisode"
            }
            AnimeStreamIdType.TMDB -> {
                val cleanTmdb = option.rawId.removePrefix("tmdb:").trim()
                if (isMovie) "tmdb:$cleanTmdb" else "tmdb:$cleanTmdb:$season:$episode"
            }
            AnimeStreamIdType.ANILIST -> {
                val cleanAnilist = option.rawId.removePrefix("anilist:").removePrefix("ani_").trim()
                if (isMovie) "anilist:$cleanAnilist" else "anilist:$cleanAnilist:$relativeEpisode"
            }
            AnimeStreamIdType.CUSTOM -> {
                val custom = option.rawId.trim()
                if (custom.contains(":")) {
                    custom
                } else if (isMovie) {
                    custom
                } else {
                    "$custom:$season:$episode"
                }
            }
        }
    }
}
