package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.nuvioShelfHoverOverdraw
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.artwork.MetaHubArtwork
import com.nuvio.app.features.details.MetaRelation
import com.nuvio.app.isDesktop

@Composable
fun DetailRelationsSection(
    relations: List<MetaRelation>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    headerHorizontalPadding: Dp = 0.dp,
    horizontalScrollPadding: Dp = 0.dp,
    onRelationClick: ((MetaRelation) -> Unit)? = null,
) {
    if (relations.isEmpty()) return

    val rowHoverInset = if (isDesktop) DetailRailHoverInset else 0.dp
    val rowEdgePadding = headerHorizontalPadding + horizontalScrollPadding + rowHoverInset

    Column(modifier = modifier.fillMaxWidth()) {
        NuvioShelfSection(
            title = if (showHeader) "Franchise & Relations" else "",
            entries = relations,
            rowModifier = Modifier
                .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                .nuvioShelfHoverOverdraw(rowHoverInset),
            headerHorizontalPadding = headerHorizontalPadding,
            rowContentPadding = PaddingValues(
                start = rowEdgePadding,
                top = rowHoverInset,
                end = rowEdgePadding,
                bottom = rowHoverInset,
            ),
            key = { relation -> "rel_${relation.id}_${relation.relationType}" },
        ) { relation ->
            RelationPosterCard(
                relation = relation,
                onClick = onRelationClick?.let { { it(relation) } },
            )
        }
    }
}

@Composable
private fun RelationPosterCard(
    relation: MetaRelation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
    val baseWidth = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    var lazyLogoUrl by remember(relation.id) { mutableStateOf<String?>(null) }
    var lazyMalScore by remember(relation.id) { mutableStateOf<Double?>(null) }
    var lazyPosterUrl by remember(relation.id, relation.poster) {
        mutableStateOf(relation.poster?.takeIf { it.isNotBlank() })
    }

    LaunchedEffect(relation.id, relation.poster) {
        if (lazyPosterUrl.isNullOrBlank()) {
            val anilistId = com.nuvio.app.features.anilist.AnilistTrackerCoordinator.extractAnilistId(relation.id)
            if (anilistId != null) {
                val media = com.nuvio.app.features.anilist.AnilistApi.getCachedMedia(anilistId)
                    ?: runCatching { com.nuvio.app.features.anilist.AnilistApi.fetchMediaById(anilistId) }.getOrNull()
                val fetchedPoster = media?.coverImage?.bestUrl
                if (!fetchedPoster.isNullOrBlank()) {
                    lazyPosterUrl = fetchedPoster
                }
            }
        }
    }

    LaunchedEffect(relation.id, anilistPrefs.showPosterTitleLogos) {
        if (anilistPrefs.showPosterTitleLogos && lazyLogoUrl == null) {
            lazyLogoUrl = MetaHubArtwork.resolveLogoUrl(relation.id)
        }
    }

    LaunchedEffect(relation.id, anilistPrefs.showPosterMalScore) {
        if (anilistPrefs.showPosterMalScore && lazyMalScore == null) {
            lazyMalScore = MetaHubArtwork.resolveMalScore(relation.id)
        }
    }

    val detailSubtitle = listOfNotNull(
        relation.format?.takeIf { it.isNotBlank() },
        relation.episodes?.takeIf { it > 0 }?.let { "$it eps" },
    ).joinToString(" • ").ifBlank { null }

    Box(modifier = modifier) {
        NuvioPosterCard(
            title = relation.title,
            imageUrl = lazyPosterUrl ?: relation.poster,
            basePosterWidthDp = baseWidth,
            shape = NuvioPosterShape.Poster,
            detailLine = detailSubtitle,
            showTitleBelow = !posterCardStyle.hideLabelsEnabled,
            bottomLeftLogoUrl = if (anilistPrefs.showPosterTitleLogos) lazyLogoUrl else null,
            anilistScore = if (anilistPrefs.showPosterAnilistScore) relation.averageScore?.toDouble() else null,
            malScore = if (anilistPrefs.showPosterMalScore) lazyMalScore else null,
            scoreFormat = anilistPrefs.posterScoreFormat,
            onClick = onClick,
        )

        // Relation Type Badge (Prequel, Sequel, Side Story, Movie, etc.)
        Box(
            modifier = Modifier
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (relation.relationType.uppercase()) {
                        "PREQUEL" -> MaterialTheme.colorScheme.tertiary
                        "SEQUEL" -> MaterialTheme.colorScheme.primary
                        "MOVIE" -> Color(0xFFE50914)
                        "PARENT", "PARENT STORY" -> Color(0xFF6366F1)
                        "SIDE STORY", "SIDE_STORY", "SPIN_OFF", "SPIN-OFF" -> Color(0xFF10B981)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .align(Alignment.TopStart),
        ) {
            Text(
                text = relation.relationType,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                ),
                color = when (relation.relationType.uppercase()) {
                    "PREQUEL" -> MaterialTheme.colorScheme.onTertiary
                    "SEQUEL" -> MaterialTheme.colorScheme.onPrimary
                    "MOVIE", "PARENT", "PARENT STORY", "SIDE STORY", "SIDE_STORY", "SPIN_OFF", "SPIN-OFF" -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
        }
    }
}

private val DetailRailHoverInset = 20.dp
