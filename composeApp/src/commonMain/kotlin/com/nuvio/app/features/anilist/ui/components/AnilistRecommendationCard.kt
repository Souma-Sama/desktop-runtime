package com.nuvio.app.features.anilist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.artwork.MetaHubArtwork
import com.nuvio.app.features.anilist.recommendations.AnilistRecommendation

@Composable
fun AnilistRecommendationCard(
    recommendation: AnilistRecommendation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
    val baseWidth = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    val itemMetaId = "anilist:${recommendation.mediaId}"

    var lazyLogoUrl by remember(itemMetaId) { mutableStateOf<String?>(null) }
    var lazyMalScore by remember(itemMetaId) { mutableStateOf<Double?>(null) }
    var lazyPosterUrl by remember(recommendation.mediaId, recommendation.coverImage) {
        mutableStateOf(recommendation.coverImage?.takeIf { it.isNotBlank() })
    }

    LaunchedEffect(itemMetaId, anilistPrefs.showPosterTitleLogos) {
        if (anilistPrefs.showPosterTitleLogos && lazyLogoUrl == null) {
            lazyLogoUrl = MetaHubArtwork.resolveLogoUrl(itemMetaId)
        }
    }

    LaunchedEffect(itemMetaId, anilistPrefs.showPosterMalScore) {
        if (anilistPrefs.showPosterMalScore && lazyMalScore == null) {
            lazyMalScore = MetaHubArtwork.resolveMalScore(itemMetaId)
        }
    }

    val detailSubtitle = listOfNotNull(
        recommendation.format?.takeIf { it.isNotBlank() },
        recommendation.episodes?.takeIf { it > 0 }?.let { "$it eps" },
    ).joinToString(" • ").ifBlank { null }

    Box(modifier = modifier) {
        NuvioPosterCard(
            title = recommendation.title,
            imageUrl = lazyPosterUrl ?: recommendation.coverImage,
            basePosterWidthDp = baseWidth,
            shape = NuvioPosterShape.Poster,
            detailLine = detailSubtitle,
            showTitleBelow = !posterCardStyle.hideLabelsEnabled,
            bottomLeftLogoUrl = if (anilistPrefs.showPosterTitleLogos) lazyLogoUrl else null,
            anilistScore = if (anilistPrefs.showPosterAnilistScore) recommendation.averageScore?.toDouble() else null,
            malScore = if (anilistPrefs.showPosterMalScore) lazyMalScore else null,
            scoreFormat = anilistPrefs.posterScoreFormat,
            onClick = onClick,
        )

        // Community Agreement Badge (Top End)
        if (recommendation.rating > 0) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .align(Alignment.TopEnd),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (recommendation.rating >= 1000) "${recommendation.rating / 1000}k" else "${recommendation.rating}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}
