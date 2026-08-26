package com.nuvio.app.features.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AnilistSectionSettings(
    val type: String,
    val enabled: Boolean = true,
)

@Serializable
data class AnilistPreferences(
    val autoMarkEpisodeWatched: Boolean = true,
    val watchedPercentageThreshold: Int = 85,
    val autoMoveToWatchingOnStart: Boolean = true,
    val autoCompleteOnLastEpisode: Boolean = true,
    val autoAddNewAnime: Boolean = true,
    val showSyncNotification: Boolean = true,
    val preferredTitleLanguage: AnilistTitleLanguage = AnilistTitleLanguage.ROMAJI,
    val preferredScoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_10_DECIMAL,
    val librarySections: List<AnilistSectionSettings> = defaultLibrarySections,
) {
    companion object {
        val defaultLibrarySections = listOf(
            AnilistSectionSettings("Watching", enabled = true),
            AnilistSectionSettings("Planning", enabled = true),
            AnilistSectionSettings("Completed", enabled = true),
            AnilistSectionSettings("Rewatching", enabled = true),
            AnilistSectionSettings("Paused", enabled = true),
            AnilistSectionSettings("Dropped", enabled = false),
        )
    }
}

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
