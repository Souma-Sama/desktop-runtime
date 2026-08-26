package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.nuvio.app.features.details.MetaDetails

@Composable
internal fun DesktopStripePlaceholder(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    colorScheme.surfaceVariant.copy(alpha = 0.48f),
                    colorScheme.background,
                    colorScheme.surfaceVariant.copy(alpha = 0.48f),
                ),
            ),
        ),
    )
}

internal fun desktopYearLabel(meta: MetaDetails): String? =
    com.nuvio.app.features.details.formatMetaReleaseLineForDetails(meta)
        ?: meta.releaseInfo?.trim()

internal fun desktopSeasonCountLabel(meta: MetaDetails): String? {
    if (meta.type.equals("movie", ignoreCase = true)) return null
    val isAnilist = meta.id.startsWith("ani_", ignoreCase = true) || meta.id.startsWith("anilist:", ignoreCase = true)
    val seasons = meta.videos.mapNotNull { it.season }.toSet()
    if (seasons.isEmpty()) return null

    if (isAnilist) {
        val singleSeason = seasons.firstOrNull() ?: 1
        return when {
            singleSeason == 0 -> "Special"
            singleSeason > 1 -> "Season $singleSeason"
            else -> {
                val title = meta.name.lowercase()
                val hasExplicitSeason1 = title.contains("season 1") || title.contains("1st season") ||
                                         title.contains("part 1") || title.contains("cour 1") ||
                                         title.contains("first season")
                if (hasExplicitSeason1) "Season 1" else "1 Season"
            }
        }
    }

    val validSeasons = seasons.filter { it > 0 }
    return when {
        validSeasons.size == 1 && validSeasons.first() > 1 -> "Season ${validSeasons.first()}"
        validSeasons.size == 1 -> "1 Season"
        validSeasons.size > 1 -> "${validSeasons.size} Seasons"
        seasons.contains(0) -> "Special"
        else -> null
    }
}
