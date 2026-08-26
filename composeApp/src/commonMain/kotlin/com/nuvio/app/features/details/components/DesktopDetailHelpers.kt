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
    meta.releaseInfo
        ?.trim()
        ?.takeIf { it.length >= 4 }
        ?.take(4)

internal fun desktopSeasonCountLabel(meta: MetaDetails): String? {
    if (meta.type.equals("movie", ignoreCase = true)) return null
    val isAnilist = meta.id.startsWith("ani_", ignoreCase = true) || meta.id.startsWith("anilist:", ignoreCase = true)
    val seasons = meta.videos.mapNotNull { it.season }.toSet()
    if (seasons.isEmpty()) return null

    if (isAnilist) {
        val singleSeason = seasons.firstOrNull() ?: 1
        return when {
            singleSeason == 0 -> "Special"
            else -> "Season $singleSeason"
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
