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

    val deco = com.nuvio.app.features.anilist.KaiHooks.rememberPosterDecorations(item)
    var lazyLogoUrl by remember(item.id) { mutableStateOf(item.logo ?: MetaHubArtwork.getLogoUrl(item.id)) }
    var lazyMalScore by remember(item.id) { mutableStateOf(item.malScore ?: MetaHubArtwork.getMalScore(item.id)) }

    LaunchedEffect(item.id, deco.showTitleLogos) {
        if (deco.showTitleLogos && lazyLogoUrl == null) {
            lazyLogoUrl = MetaHubArtwork.resolveLogoUrl(item.id)
        }
    }

    LaunchedEffect(item.id, deco.showMalScore) {
        if (deco.showMalScore && lazyMalScore == null) {
            lazyMalScore = MetaHubArtwork.resolveMalScore(item.id)
        }
    }

    val effectiveLogoUrl = if (deco.showTitleLogos) lazyLogoUrl else null
    val effectiveMalScore = if (deco.showMalScore) lazyMalScore else null

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
            anilistScore = deco.anilistScore,
            malScore = effectiveMalScore,
            scoreFormat = anilistPrefs.posterScoreFormat,
            anilistStatus = deco.libraryStatus,
            anilistProgress = deco.libraryProgress,
            anilistUserScore = deco.userScore,
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
