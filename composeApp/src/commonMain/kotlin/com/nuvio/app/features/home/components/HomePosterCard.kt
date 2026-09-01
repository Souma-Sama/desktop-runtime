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

    val isAnime = remember(item.id, item.name, item.genres, item.type) {
        com.nuvio.app.features.anilist.AnilistTrackerCoordinator.isAnimeCandidate(
            title = item.name,
            genres = item.genres,
            country = null,
            language = null,
            mediaId = item.id,
            type = item.type,
        )
    }

    // Lazy load MAL score for visible anime items only
    LaunchedEffect(item.id, anilistPrefs.showPosterMalScore, isAnime) {
        if (anilistPrefs.enabled && anilistPrefs.showPosterMalScore && isAnime && lazyMalScore == null) {
            lazyMalScore = MetaHubArtwork.resolveMalScore(item.id)
        }
    }

    val effectiveLogoUrl = if (anilistPrefs.enabled && anilistPrefs.showPosterTitleLogos && isAnime) lazyLogoUrl else null
    val effectiveAnilistScore = if (anilistPrefs.enabled && anilistPrefs.showPosterAnilistScore && isAnime) item.anilistScore else null
    val effectiveMalScore = if (anilistPrefs.enabled && anilistPrefs.showPosterMalScore && isAnime) lazyMalScore else null

    val anilistLibraryState by com.nuvio.app.features.anilist.AnilistLibraryRepository.uiState.collectAsStateWithLifecycle()
    val mediaStatus = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge && isAnime) {
        com.nuvio.app.features.anilist.AnilistLibraryRepository.getMediaStatusById(item.id, item.name)
    } else null
    val mediaProgress = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge && isAnime) {
        com.nuvio.app.features.anilist.AnilistLibraryRepository.getMediaProgressById(item.id, item.name)
    } else null
    val mediaUserScore = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge && isAnime) {
        com.nuvio.app.features.anilist.AnilistLibraryRepository.getUserScoreById(item.id, item.name)
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
            anilistUserScore = mediaUserScore,
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
