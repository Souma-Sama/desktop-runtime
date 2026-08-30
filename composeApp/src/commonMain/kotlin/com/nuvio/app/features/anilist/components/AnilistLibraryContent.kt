package com.nuvio.app.features.anilist.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.features.anilist.AnilistLibraryItem
import com.nuvio.app.features.anilist.AnilistLibraryUiState
import com.nuvio.app.features.anilist.AnilistSectionSettings
import com.nuvio.app.features.anilist.AnilistSortBy
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomeSkeletonRow

fun LazyListScope.anilistLibraryContent(
    uiState: AnilistLibraryUiState,
    sectionsConfig: List<AnilistSectionSettings>,
    sortBy: AnilistSortBy,
    sortAscending: Boolean,
    onPosterClick: (AnilistLibraryItem) -> Unit,
    onConnectAnilistClick: () -> Unit,
    onRefresh: () -> Unit,
    isOffline: Boolean,
) {
    when {
        uiState.isLoading && !uiState.isLoaded -> {
            items(3) {
                HomeSkeletonRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        !uiState.isLoaded -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "AniList Not Connected",
                    message = "Connect your AniList account in Settings to view your custom anime shelves here.",
                    actionLabel = "Connect Now",
                    onActionClick = onConnectAnilistClick,
                )
            }
        }

        isOffline -> {
            item {
                HomeEmptyStateCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = "You are Offline",
                    message = "Internet connection is required to view your AniList library shelves.",
                    actionLabel = "Retry",
                    onActionClick = onRefresh,
                )
            }
        }

        else -> {
            val sections = sectionsConfig.mapNotNull { sectionConfig ->
                if (!sectionConfig.enabled) return@mapNotNull null
                val rawList = when (sectionConfig.type.lowercase().trim()) {
                    "watching", "currently watching", "current" -> uiState.watching
                    "completed" -> uiState.completed
                    "planning", "plan to watch" -> uiState.planning
                    "paused", "on hold" -> uiState.paused
                    "dropped" -> uiState.dropped
                    "rewatching", "repeating" -> uiState.rewatching
                    else -> emptyList()
                }
                val sorted = rawList.sortedWith(
                    when (sortBy) {
                        AnilistSortBy.LAST_UPDATED -> compareBy { it.updatedAt }
                        AnilistSortBy.SCORE -> compareBy { it.score ?: 0.0 }
                        AnilistSortBy.TITLE -> compareBy { it.title.lowercase() }
                        AnilistSortBy.RELEASE_DATE -> compareBy { it.updatedAt }
                    },
                ).let { if (sortAscending) it else it.reversed() }
                Pair(sectionConfig.type, sorted)
            }

            var displayedAnySection = false

            sections.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    displayedAnySection = true
                    item(key = "anilist-shelf:$title") {
                        NuvioShelfSection(
                            title = title,
                            entries = list,
                            headerHorizontalPadding = 16.dp,
                            rowContentPadding = PaddingValues(horizontal = 16.dp),
                            key = { entry -> entry.id },
                        ) { entry ->
                            HomePosterCard(
                                item = entry.toMetaPreview(),
                                isWatched = entry.status.equals("COMPLETED", ignoreCase = true),
                                onClick = { onPosterClick(entry) },
                            )
                        }
                    }
                }
            }

            if (!displayedAnySection) {
                item {
                    HomeEmptyStateCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = "Your Lists are Empty",
                        message = "Start watching or planning anime on AniList to see them show up here.",
                        actionLabel = "Refresh Now",
                        onActionClick = onRefresh,
                    )
                }
            }
        }
    }
}

private fun AnilistLibraryItem.toMetaPreview(): MetaPreview =
    MetaPreview(
        id = "anilist:$id",
        type = if (format?.equals("MOVIE", ignoreCase = true) == true) "movie" else "series",
        name = title,
        poster = posterUrl,
        banner = posterUrl,
        logo = null,
        description = null,
        releaseInfo = if (progress > 0 && totalEpisodes != null) "Ep $progress / $totalEpisodes" else if (progress > 0) "Ep $progress" else null,
        posterShape = PosterShape.Poster,
    )
