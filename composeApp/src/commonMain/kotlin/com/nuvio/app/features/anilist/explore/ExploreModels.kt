package com.nuvio.app.features.anilist.explore

import androidx.compose.ui.graphics.vector.ImageVector
import com.nuvio.app.features.anilist.AnilistAdvancedFilterState
import com.nuvio.app.features.anilist.AnilistSortOption
import com.nuvio.app.features.anilist.calendar.AnimeSeason

enum class ExploreCategory(
    val title: String,
    val icon: ImageVector,
    val isManga: Boolean,
) {
    // --- Anime Categories ---
    TOP_100_ANIME("Top 100", ExploreIcons.Star, isManga = false),
    TOP_POPULAR_ANIME("Top Popular", ExploreIcons.TrendingUp, isManga = false),
    UPCOMING_ANIME("Upcoming", ExploreIcons.Schedule, isManga = false),
    AIRING_ANIME("Airing", ExploreIcons.RssFeed, isManga = false),
    SPRING_ANIME("Spring", ExploreIcons.LocalFlorist, isManga = false),
    SUMMER_ANIME("Summer", ExploreIcons.Sunny, isManga = false),
    FALL_ANIME("Fall", ExploreIcons.Rainy, isManga = false),
    WINTER_ANIME("Winter", ExploreIcons.AcUnit, isManga = false),
    TOP_MOVIES_ANIME("Top Movies", ExploreIcons.Movie, isManga = false),
    CALENDAR("Calendar", ExploreIcons.Calendar, isManga = false),

    // --- Manga Categories ---
    TOP_100_MANGA("Top 100", ExploreIcons.Star, isManga = true),
    TOP_POPULAR_MANGA("Top Popular", ExploreIcons.TrendingUp, isManga = true),
    UPCOMING_MANGA("Upcoming", ExploreIcons.Schedule, isManga = true),
    PUBLISHING_MANGA("Publishing", ExploreIcons.RssFeed, isManga = true);

    val fullDisplayTitle: String
        get() = when (this) {
            SPRING_ANIME -> "Spring ${AnimeSeason.currentYear()} Anime"
            SUMMER_ANIME -> "Summer ${AnimeSeason.currentYear()} Anime"
            FALL_ANIME -> "Fall ${AnimeSeason.currentYear()} Anime"
            WINTER_ANIME -> "Winter ${AnimeSeason.currentYear()} Anime"
            TOP_100_ANIME -> "Top 100 Anime"
            TOP_POPULAR_ANIME -> "Top Popular Anime"
            UPCOMING_ANIME -> "Upcoming Anime"
            AIRING_ANIME -> "Airing Anime"
            TOP_MOVIES_ANIME -> "Top Anime Movies"
            CALENDAR -> "Anime Release Calendar"
            TOP_100_MANGA -> "Top 100 Manga"
            TOP_POPULAR_MANGA -> "Top Popular Manga"
            UPCOMING_MANGA -> "Upcoming Manga"
            PUBLISHING_MANGA -> "Publishing Manga"
        }

    fun toFilterState(): AnilistAdvancedFilterState {
        val currentYear = AnimeSeason.currentYear()
        return when (this) {
            TOP_100_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                sort = AnilistSortOption.SCORE,
            )
            TOP_POPULAR_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                sort = AnilistSortOption.POPULARITY,
            )
            UPCOMING_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                statuses = setOf("NOT_YET_RELEASED"),
                sort = AnilistSortOption.POPULARITY,
            )
            AIRING_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                statuses = setOf("RELEASING"),
                sort = AnilistSortOption.POPULARITY,
            )
            SPRING_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                season = "SPRING",
                seasonYear = currentYear,
                sort = AnilistSortOption.POPULARITY,
            )
            SUMMER_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                season = "SUMMER",
                seasonYear = currentYear,
                sort = AnilistSortOption.POPULARITY,
            )
            FALL_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                season = "FALL",
                seasonYear = currentYear,
                sort = AnilistSortOption.POPULARITY,
            )
            WINTER_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                season = "WINTER",
                seasonYear = currentYear,
                sort = AnilistSortOption.POPULARITY,
            )
            TOP_MOVIES_ANIME -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                formats = setOf("MOVIE"),
                sort = AnilistSortOption.SCORE,
            )
            CALENDAR -> AnilistAdvancedFilterState(
                searchType = "ANIME",
                statuses = setOf("RELEASING"),
                sort = AnilistSortOption.POPULARITY,
            )
            TOP_100_MANGA -> AnilistAdvancedFilterState(
                searchType = "MANGA",
                sort = AnilistSortOption.SCORE,
            )
            TOP_POPULAR_MANGA -> AnilistAdvancedFilterState(
                searchType = "MANGA",
                sort = AnilistSortOption.POPULARITY,
            )
            UPCOMING_MANGA -> AnilistAdvancedFilterState(
                searchType = "MANGA",
                statuses = setOf("NOT_YET_RELEASED"),
                sort = AnilistSortOption.POPULARITY,
            )
            PUBLISHING_MANGA -> AnilistAdvancedFilterState(
                searchType = "MANGA",
                statuses = setOf("RELEASING"),
                sort = AnilistSortOption.POPULARITY,
            )
        }
    }
}
