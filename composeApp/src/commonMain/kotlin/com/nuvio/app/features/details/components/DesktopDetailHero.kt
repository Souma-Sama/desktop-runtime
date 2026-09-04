package com.nuvio.app.features.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioDesktopImageScaling
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.DesktopBackdropVerticalBias
import com.nuvio.app.core.ui.StandardDesktopViewportAspectRatio
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.desktopPageHorizontalPaddingForWidth
import com.nuvio.app.core.ui.fullscreenActionHorizontalInsetForWidth
import com.nuvio.app.core.ui.expandingWideArtworkWidthDp
import com.nuvio.app.core.ui.isFullscreenActionSupported
import com.nuvio.app.core.ui.WideDesktopViewportAspectRatio
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.tmdb.originalTmdbImageUrl
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.detail_logo_content_description
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopDetailBackdrop(
    meta: MetaDetails,
    viewportHeight: Dp,
    scrollOffset: () -> Int = { 0 },
    heroTrailerSourceUrl: String?,
    heroTrailerSourceAudioUrl: String?,
    heroTrailerReady: Boolean,
    heroTrailerPlayWhenReady: Boolean,
    heroTrailerMuted: Boolean,
    heroGradientColor: Color? = null,
    blurBackdrop: Boolean = false,
    onBackdropLoaded: (Painter) -> Unit = {},
    onHeroTrailerReady: () -> Unit,
    onHeroTrailerEnded: () -> Unit,
    onHeroTrailerError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val sideGradientColor = heroGradientColor ?: colorScheme.background
    val opacity = NuvioTokens.Opacity
    val trailerAlpha by animateFloatAsState(
        targetValue = if (heroTrailerReady) 1f else 0f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.sheetEnterMillis),
        label = "desktop_detail_hero_trailer_alpha",
    )
    val gradientIntensity by animateFloatAsState(
        targetValue = if (heroTrailerReady) 0.3f else 1f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.sheetEnterMillis),
        label = "desktop_detail_hero_gradient_intensity",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(sideGradientColor),
    ) {
        val aspectRatio = maxWidth.value / viewportHeight.value
        val useWideArtworkFrame = aspectRatio > StandardDesktopViewportAspectRatio
        val cropSideBackdrop = aspectRatio > WideDesktopViewportAspectRatio
        val backdropWidth = if (useWideArtworkFrame) {
            expandingWideArtworkWidthDp(maxWidth.value, viewportHeight.value).dp
        } else {
            maxWidth
        }
        val backdropModifier = Modifier
            .align(Alignment.CenterEnd)
            .width(backdropWidth)
            .fillMaxHeight()
        val artworkModifier = if (blurBackdrop) backdropModifier.blur(30.dp) else backdropModifier
        val parallaxArtworkModifier = artworkModifier.graphicsLayer {
            translationY = -scrollOffset() * 0.45f
            scaleX = 1.05f
            scaleY = 1.05f
        }
        val isKai = com.nuvio.app.features.anilist.KaiHooks.isKaiMedia(meta.id)
        val imageUrl = meta.background ?: meta.poster
        val isAnilistBanner = imageUrl?.contains("anilistcdn/media/anime/banner", ignoreCase = true) == true
        val isAnimeMedia = isKai || isAnilistBanner

        val bottomSpreadStrength = if (isAnimeMedia) 0f else ((21f / 9f - aspectRatio) / (5f / 9f)).coerceIn(0f, 1f)
        val baseSideFade = if (isAnimeMedia) {
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to sideGradientColor.copy(alpha = 0.88f * gradientIntensity),
                    0.08f to sideGradientColor.copy(alpha = 0.65f * gradientIntensity),
                    0.18f to sideGradientColor.copy(alpha = 0.32f * gradientIntensity),
                    0.30f to sideGradientColor.copy(alpha = 0.10f * gradientIntensity),
                    0.45f to Color.Transparent,
                    1.00f to Color.Transparent,
                ),
            )
        } else {
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to sideGradientColor,
                    0.12f to sideGradientColor.copy(alpha = 0.98f * gradientIntensity),
                    0.34f to sideGradientColor.copy(alpha = opacity.overlayHeavy * gradientIntensity),
                    0.62f to sideGradientColor.copy(alpha = opacity.overlayLight * gradientIntensity),
                    0.86f to sideGradientColor.copy(alpha = opacity.subtle * gradientIntensity),
                    1.00f to Color.Transparent,
                ),
            )
        }
        if (imageUrl != null) {
            if (isAnilistBanner) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = backdropModifier
                        .blur(36.dp)
                        .graphicsLayer {
                            translationY = -scrollOffset() * 0.45f
                            scaleX = 1.05f
                            scaleY = 1.05f
                            clip = true
                        },
                    alignment = BiasAlignment(0f, DesktopBackdropVerticalBias),
                    contentScale = ContentScale.Crop,
                    desktopImageScaling = NuvioDesktopImageScaling.Disabled,
                )
            }
            AsyncImage(
                model = originalTmdbImageUrl(imageUrl),
                contentDescription = meta.name,
                modifier = if (isAnilistBanner) {
                    parallaxArtworkModifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.12f to Color.Black,
                                        0.88f to Color.Black,
                                        1.00f to Color.Transparent,
                                    ),
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else {
                    parallaxArtworkModifier
                },
                alignment = BiasAlignment(0f, DesktopBackdropVerticalBias),
                contentScale = if (useWideArtworkFrame && !cropSideBackdrop) ContentScale.Fit else ContentScale.Crop,
                desktopImageScaling = NuvioDesktopImageScaling.Disabled,
                onSuccess = { state -> onBackdropLoaded(state.painter) },
            )
        } else {
            DesktopStripePlaceholder(modifier = backdropModifier)
        }

        if (heroTrailerSourceUrl != null) {
            HeroTrailerPlayerSurface(
                sourceUrl = heroTrailerSourceUrl,
                sourceAudioUrl = heroTrailerSourceAudioUrl,
                playWhenReady = heroTrailerPlayWhenReady,
                muted = heroTrailerMuted,
                fillFrame = true,
                modifier = parallaxArtworkModifier.graphicsLayer { alpha = trailerAlpha },
                onReady = onHeroTrailerReady,
                onEnded = onHeroTrailerEnded,
                onError = onHeroTrailerError,
            )
        }

        Box(
            modifier = backdropModifier
                .drawWithCache {
                    val bottomSpread = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to sideGradientColor.copy(alpha = 0.12f * gradientIntensity * bottomSpreadStrength),
                            0.55f to sideGradientColor.copy(alpha = 0.12f * gradientIntensity * bottomSpreadStrength),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset(0f, size.height * 1.15f),
                        radius = size.width * 0.72f,
                    )
                    onDrawBehind {
                        drawRect(baseSideFade)
                        if (bottomSpreadStrength > 0f) drawRect(bottomSpread)
                    }
                },
        )

        // Seamless vertical bottom fade to completely eliminate any sharp backdrop/banner cutoff
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val baseBottomFade = if (isAnimeMedia) {
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.50f to Color.Transparent,
                                0.66f to sideGradientColor.copy(alpha = 0.28f * gradientIntensity),
                                0.82f to sideGradientColor.copy(alpha = 0.70f * gradientIntensity),
                                0.95f to sideGradientColor,
                                1.00f to sideGradientColor,
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.30f to Color.Transparent,
                                0.48f to sideGradientColor.copy(alpha = 0.25f * gradientIntensity),
                                0.65f to sideGradientColor.copy(alpha = 0.65f * gradientIntensity),
                                0.82f to sideGradientColor.copy(alpha = 0.92f * gradientIntensity),
                                0.95f to sideGradientColor,
                                1.00f to sideGradientColor,
                            ),
                        )
                    }
                    onDrawBehind {
                        drawRect(baseBottomFade)
                    }
                },
        )
    }
}

@Composable
fun DesktopDetailHero(
    meta: MetaDetails,
    title: String? = null,
    playButtonLabel: String,
    isSaved: Boolean,
    isWatched: Boolean,
    onHeightChanged: (Int) -> Unit,
    heroTrailerSourceUrl: String?,
    heroTrailerReady: Boolean,
    heroTrailerMuted: Boolean,
    onHeroTrailerMuteToggle: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val space = NuvioTokens.Space
    val trailerAlpha by animateFloatAsState(
        targetValue = if (heroTrailerReady) 1f else 0f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.sheetEnterMillis),
        label = "desktop_detail_hero_controls_alpha",
    )
    var logoLoadError by remember(meta.id, meta.logo) { mutableStateOf(false) }
    val logoUrl = meta.logo?.takeIf { it.isNotBlank() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(660.dp)
            .onSizeChanged { onHeightChanged(it.height) },
    ) {
        val actionHorizontalInset = fullscreenActionHorizontalInsetForWidth(maxWidth.value)
        val pageHorizontalPadding = desktopPageHorizontalPaddingForWidth(maxWidth.value)

        val isNarrow = maxWidth < 600.dp
        val isAnilistItem = com.nuvio.app.features.anilist.KaiHooks.isKaiMedia(meta.id)
        val heroMinHeight = 660.dp
        val logoMaxHeight = if (isNarrow && isAnilistItem) (heroMinHeight * 0.22f).coerceIn(48.dp, 64.dp)
                            else if (isNarrow) 80.dp
                            else 120.dp
        val logoMaxWidth = if (isNarrow && isAnilistItem) (maxWidth * 0.55f).coerceIn(160.dp, 280.dp)
                           else if (isNarrow) 360.dp
                           else 560.dp

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = 760.dp)
                .padding(
                    start = pageHorizontalPadding,
                    end = space.s32,
                    bottom = space.s40,
                ),
        ) {
            val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
            val heroLogoScale = anilistPrefs.heroTitleLogoScale
            if (logoUrl != null && !logoLoadError) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = stringResource(Res.string.detail_logo_content_description, title ?: meta.name),
                    modifier = Modifier
                        .widthIn(max = logoMaxWidth * heroLogoScale)
                        .height(logoMaxHeight * heroLogoScale),
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Fit,
                    onError = { logoLoadError = true },
                )
            } else {
                Text(
                    text = title ?: meta.name,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = NuvioTokens.Type.displayMd,
                        lineHeight = NuvioTokens.LineHeight.displayMd,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = NuvioTokens.LetterSpacing.none,
                    ),
                    color = colorScheme.onBackground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(space.s20))
            DesktopHeroMetaRow(meta = meta)
            if (meta.externalRatings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(space.s12))
                DetailRatingsRow(
                    ratings = meta.externalRatings,
                    modifier = Modifier.widthIn(max = 520.dp),
                )
            }
            if (meta.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(space.s12))
                Text(
                    text = meta.genres.take(4).joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = NuvioTokens.Type.bodyMd,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = NuvioTokens.LetterSpacing.none,
                    ),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            meta.description?.takeIf { it.isNotBlank() }?.let { synopsis ->
                Spacer(modifier = Modifier.height(space.s16))
                ExpandableDescription(
                    text = synopsis,
                    collapsedMaxLines = 3,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = NuvioTokens.Type.bodyLg,
                        lineHeight = NuvioTokens.LineHeight.bodyLg,
                        letterSpacing = NuvioTokens.LetterSpacing.none,
                    ),
                    color = colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(space.s28))
            DetailActionButtons(
                modifier = Modifier.widthIn(max = 520.dp),
                meta = meta,
                title = title,
                playLabel = playButtonLabel,
                secondaryActions = listOf(
                    DetailSecondaryAction(
                        label = if (isWatched) {
                            stringResource(Res.string.hero_mark_unwatched)
                        } else {
                            stringResource(Res.string.hero_mark_watched)
                        },
                        icon = if (isWatched) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.CheckCircleOutline
                        },
                        isActive = isWatched,
                        onClick = onWatchedClick,
                    ),
                    DetailSecondaryAction(
                        label = if (isSaved) {
                            stringResource(Res.string.hero_remove_from_library)
                        } else {
                            stringResource(Res.string.hero_add_to_library)
                        },
                        icon = if (isSaved) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Add
                        },
                        isActive = isSaved,
                        onClick = onSaveClick,
                        onLongClick = onSaveLongClick,
                    ),
                ),
                isTablet = true,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
            )
        }

        if (heroTrailerSourceUrl != null) {
            Surface(
                onClick = onHeroTrailerMuteToggle,
                enabled = heroTrailerReady,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = space.s32,
                        end = actionHorizontalInset + if (isFullscreenActionSupported) 60.dp else 0.dp,
                    )
                    .size(48.dp)
                    .graphicsLayer { alpha = trailerAlpha },
                shape = CircleShape,
                color = colorScheme.surfaceVariant.copy(alpha = 0.82f),
                contentColor = colorScheme.onSurface,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (heroTrailerMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        if (isFullscreenActionSupported) {
            FullscreenActionButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = space.s32, end = actionHorizontalInset),
                buttonSize = 48.dp,
                iconSize = 24.dp,
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.82f),
                contentColor = colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DesktopHeroMetaRow(meta: MetaDetails) {
    val colorScheme = MaterialTheme.colorScheme
    val space = NuvioTokens.Space
    val opacity = NuvioTokens.Opacity
    val metaItems = buildList {
        desktopYearLabel(meta)?.let(::add)
        desktopSeasonCountLabel(meta)?.let(::add)
        formatRuntimeForDisplay(meta.runtime)?.let(::add)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space.s16),
    ) {
        metaItems.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = NuvioTokens.Type.bodyLg,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                ),
                color = colorScheme.onBackground,
                maxLines = 1,
            )
        }
        meta.ageRating?.takeIf { it.isNotBlank() }?.let { rating ->
            Box(
                modifier = Modifier
                    .border(
                        NuvioTokens.Border.thin,
                        colorScheme.onBackground.copy(alpha = opacity.overlayLight),
                        RoundedCornerShape(NuvioTokens.Radius.sm),
                    )
                    .padding(horizontal = space.s8, vertical = space.s2),
            ) {
                Text(
                    text = rating,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = NuvioTokens.Type.bodySm,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = NuvioTokens.LetterSpacing.none,
                    ),
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
        meta.nextAiringEpisode?.takeIf { it.isNotBlank() }?.let { countdown ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.18f))
                    .border(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFF10B981))
                    )
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = androidx.compose.ui.graphics.Color(0xFF34D399),
                    )
                }
            }
        }
    }
}
