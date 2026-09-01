package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.artwork.MetaHubArtwork
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled

    var lazyLogoUrl by remember(item.id) { mutableStateOf(item.logo ?: MetaHubArtwork.getLogoUrl(item.id)) }
    var lazyMalScore by remember(item.id) { mutableStateOf(item.malScore ?: MetaHubArtwork.getMalScore(item.id)) }

    // Lazy load logo for visible items only
    LaunchedEffect(item.id, anilistPrefs.showPosterTitleLogos) {
        if (anilistPrefs.showPosterTitleLogos && lazyLogoUrl == null) {
            lazyLogoUrl = MetaHubArtwork.resolveLogoUrl(item.id)
        }
    }

    // Lazy load MAL score for visible items only
    LaunchedEffect(item.id, anilistPrefs.showPosterMalScore) {
        if (anilistPrefs.showPosterMalScore && lazyMalScore == null) {
            lazyMalScore = MetaHubArtwork.resolveMalScore(item.id)
        }
    }

    val effectiveLogoUrl = if (anilistPrefs.enabled && anilistPrefs.showPosterTitleLogos) lazyLogoUrl else null
    val effectiveAnilistScore = if (anilistPrefs.enabled && anilistPrefs.showPosterAnilistScore) item.anilistScore else null
    val effectiveMalScore = if (anilistPrefs.enabled && anilistPrefs.showPosterMalScore) lazyMalScore else null

    val anilistLibraryState by com.nuvio.app.features.anilist.AnilistLibraryRepository.uiState.collectAsStateWithLifecycle()
    val mediaStatus = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge) {
        com.nuvio.app.features.anilist.AnilistLibraryRepository.getMediaStatusById(item.id)
    } else null
    val mediaProgress = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge) {
        val anilistId = com.nuvio.app.features.anilist.AnilistLibraryRepository.extractAnilistId(item.id)
        if (anilistId != null) com.nuvio.app.features.anilist.AnilistLibraryRepository.getMediaProgress(anilistId) else null
    } else null

    HomePosterHoverPreview(
        item = item,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    ) { hoverModifier ->
        NuvioPosterCard(
            title = item.name,
            imageUrl = if (isLandscapeMode) {
                (item.banner ?: MetaHubArtwork.getBackdropUrl(item.id) ?: item.poster)
            } else {
                (item.poster ?: MetaHubArtwork.getPosterUrl(item.id))
            },
            modifier = modifier.then(hoverModifier),
            basePosterWidthDp = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp),
            shape = if (isLandscapeMode) NuvioPosterShape.Landscape else item.posterShape.toNuvioPosterShape(),
            detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) null else item.releaseInfo?.let { formatReleaseDateForDisplay(it) },
            showTitleBelow = !posterCardStyle.hideLabelsEnabled,
            bottomLeftLogoUrl = effectiveLogoUrl,
            bottomLeftText = if (isLandscapeMode && effectiveLogoUrl.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
            anilistScore = effectiveAnilistScore,
            malScore = effectiveMalScore,
            scoreFormat = anilistPrefs.posterScoreFormat,
            anilistStatus = mediaStatus,
            anilistProgress = mediaProgress,
            isWatched = isWatched,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

private fun PosterShape.toNuvioPosterShape(): NuvioPosterShape =
    when (this) {
        PosterShape.Poster -> NuvioPosterShape.Poster
        PosterShape.Square -> NuvioPosterShape.Square
        PosterShape.Landscape -> NuvioPosterShape.Landscape
    }
