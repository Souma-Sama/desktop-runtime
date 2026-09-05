package com.nuvio.app.features.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AnilistSectionSettings(
    val type: String,
    val enabled: Boolean = true,
)

@Serializable
data class PendingScrobbleMutation(
    val mediaId: Int,
    val entryId: Int? = null,
    val status: String? = null,
    val progress: Int,
    val totalEpisodes: Int? = null,
    val title: String? = null,
    val posterUrl: String? = null,
    val timestampEpochMs: Long = 0L,
)

@Serializable
data class AnilistPreferences(
    val enabled: Boolean = true,
    val autoMarkEpisodeWatched: Boolean = true,
    val watchedPercentageThreshold: Int = 85,
    val autoMoveToWatchingOnStart: Boolean = true,
    val autoCompleteOnLastEpisode: Boolean = true,
    val autoAddNewAnime: Boolean = true,
    val showSyncNotification: Boolean = true,
    val preferredTitleLanguage: AnilistTitleLanguage = AnilistTitleLanguage.ROMAJI,
    val preferredScoreFormat: AnilistScoreFormat = AnilistScoreFormat.POINT_10_DECIMAL,
    val showPosterTitleLogos: Boolean = true,
    val showPosterAnilistScore: Boolean = true,
    val showPosterMalScore: Boolean = true,
    val showPosterStatusBadge: Boolean = true,
    val posterScoreFormat: AnilistPosterScoreFormat = AnilistPosterScoreFormat.PERCENTAGE,
    val posterRatingBadgeScale: Float = 1.0f,
    val posterStatusBadgeScale: Float = 1.0f,
    val posterTitleLogoScale: Float = 1.0f,
    val heroTitleLogoScale: Float = 1.0f,
    val hideAdultContent: Boolean = true,
    val enableAdvancedFilters: Boolean = true,
    val enableStatsDashboard: Boolean = true,
    val enableActivityFeed: Boolean = true,
    val enableInLibraryFilter: Boolean = true,
    val enableEpisodicDiscussions: Boolean = true,
    val librarySections: List<AnilistSectionSettings> = defaultAuthenticatedSections,
    val streamIdOverrides: Map<Int, String> = emptyMap(),
    val useFloatingGlassDesktopSidebar: Boolean = true,
    val trackerTheme: AnilistTrackerTheme = AnilistTrackerTheme.FROSTED_GLASS,
    val pendingScrobbleMutations: List<PendingScrobbleMutation> = emptyList(),
) {
    companion object {
        val defaultUnauthenticatedSections = listOf(
            AnilistSectionSettings("Trending Anime", enabled = true),
            AnilistSectionSettings("Currently Airing", enabled = true),
            AnilistSectionSettings("Popular This Season", enabled = true),
            AnilistSectionSettings("Top Rated Anime", enabled = true),
        )

        val defaultAuthenticatedSections = listOf(
            AnilistSectionSettings("Currently Watching", enabled = true),
            AnilistSectionSettings("Plan to Watch", enabled = true),
            AnilistSectionSettings("Trending Anime", enabled = true),
            AnilistSectionSettings("Currently Airing", enabled = true),
            AnilistSectionSettings("Popular This Season", enabled = true),
            AnilistSectionSettings("Top Rated Anime", enabled = true),
            AnilistSectionSettings("Completed", enabled = true),
            AnilistSectionSettings("Rewatching", enabled = true),
            AnilistSectionSettings("Paused", enabled = true),
            AnilistSectionSettings("Dropped", enabled = true),
        )

        val defaultLibrarySections = defaultAuthenticatedSections
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

@Serializable
enum class AnilistPosterScoreFormat(val label: String) {
    PERCENTAGE("Percentage (e.g. 84%)"),
    POINT_10("10-Point Score (e.g. 8.4)"),
}

@Serializable
enum class AnilistTrackerTheme(val label: String) {
    FROSTED_GLASS("Frosted Glass"),
    WATER_GLASS("Water Glass"),
    MIDNIGHT_GLASS("Midnight Glass"),
    SIDEBAR_GLASS("Sidebar Glass"),
}

