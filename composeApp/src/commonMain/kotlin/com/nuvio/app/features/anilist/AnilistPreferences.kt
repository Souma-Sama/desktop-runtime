package com.nuvio.app.features.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AnilistPreferences(
    val autoMarkEpisodeWatched: Boolean = true,
    val watchedPercentageThreshold: Int = 85,
    val autoMoveToWatchingOnStart: Boolean = true,
    val autoCompleteOnLastEpisode: Boolean = true,
    val showSyncNotification: Boolean = true,
    val preferredTitleLanguage: AnilistTitleLanguage = AnilistTitleLanguage.ROMAJI,
    val preferredScoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_10_DECIMAL,
)

@Serializable
enum class AnilistTitleLanguage(val label: String) {
    ROMAJI("Romaji"),
    ENGLISH("English"),
    NATIVE("Native (Japanese)"),
}

@Serializable
enum class AnilistScoreFormat(val label: String) {
    POINT_10_DECIMAL("10-Point Decimal (0.0 - 10.0)"),
    POINT_100("100-Point (0 - 100)"),
    POINT_5("5-Star (1 - 5)"),
    POINT_3("3-Point (Smileys)"),
}
