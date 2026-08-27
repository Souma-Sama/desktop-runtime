package com.nuvio.app.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioPosterCard
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.fanart.FanartService
import com.nuvio.app.features.fanart.FanartSettingsRepository
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
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled
    val fanartSettings = remember { FanartSettingsRepository.snapshot() }

    var dynamicPoster by remember(item.id, item.type) {
        mutableStateOf(FanartService.getCachedPoster(item.id, item.type))
    }
    var dynamicBackdrop by remember(item.id, item.type) {
        mutableStateOf(FanartService.getCachedBackdrop(item.id, item.type))
    }
    var dynamicLogo by remember(item.id, item.type) {
        mutableStateOf(FanartService.getCachedLogo(item.id, item.type))
    }

    LaunchedEffect(item.id, item.type, isLandscapeMode, fanartSettings.enabled, fanartSettings.hasApiKey) {
        if (fanartSettings.enabled && fanartSettings.hasApiKey) {
            if (isLandscapeMode) {
                if (dynamicBackdrop == null) {
                    val resolvedLogo = FanartService.resolveLogo(item.id, item.type)
                    val resolvedBackdrop = FanartService.getCachedBackdrop(item.id, item.type)
                    if (resolvedBackdrop != null) {
                        dynamicBackdrop = resolvedBackdrop
                    }
                    if (resolvedLogo != null) {
                        dynamicLogo = resolvedLogo
                    }
                }
            } else {
                if (dynamicPoster == null && fanartSettings.usePosters) {
                    val resolvedPoster = FanartService.resolvePoster(item.id, item.type)
                    if (resolvedPoster != null) {
                        dynamicPoster = resolvedPoster
                    }
                }
            }
        }
    }

    HomePosterHoverPreview(
        item = item,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    ) { hoverModifier ->
        NuvioPosterCard(
            title = item.name,
            imageUrl = if (isLandscapeMode) {
                (dynamicBackdrop ?: FanartService.getCachedBanner(item.id, item.type) ?: item.banner ?: item.poster)
            } else {
                (dynamicPoster ?: item.poster)
            },
            modifier = modifier.then(hoverModifier),
            basePosterWidthDp = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp),
            shape = if (isLandscapeMode) NuvioPosterShape.Landscape else item.posterShape.toNuvioPosterShape(),
            detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) null else item.releaseInfo?.let { formatReleaseDateForDisplay(it) },
            showTitleBelow = !posterCardStyle.hideLabelsEnabled,
            bottomLeftLogoUrl = if (isLandscapeMode) (dynamicLogo ?: item.logo) else null,
            bottomLeftText = if (isLandscapeMode && (dynamicLogo ?: item.logo).isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
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
