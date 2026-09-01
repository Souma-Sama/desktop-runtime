package com.nuvio.app.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.nuvio.app.features.anilist.AnilistPosterScoreFormat
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_view_all
import nuvio.composeapp.generated.resources.poster_logo_content_description
import nuvio.composeapp.generated.resources.rating_anilist
import nuvio.composeapp.generated.resources.rating_mal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

enum class NuvioPosterShape {
    Poster,
    Square,
    Landscape,
}

enum class NuvioViewAllPillSize {
    Default,
    Compact,
}

@Composable
fun <T> NuvioShelfSection(
    title: String,
    entries: List<T>,
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    headerHorizontalPadding: Dp = 0.dp,
    rowContentPadding: PaddingValues = PaddingValues(0.dp),
    itemSpacing: Dp = 10.dp,
    onViewAllClick: (() -> Unit)? = null,
    viewAllPillSize: NuvioViewAllPillSize = NuvioViewAllPillSize.Default,
    key: ((T) -> Any)? = null,
    animatePlacement: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    itemContent: @Composable (T) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val duplicateSafeEntries = remember(entries, key) {
        key?.let { entries.withDuplicateSafeLazyKeys(it) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap + NuvioTokens.Space.s2),
    ) {
        if (title.isNotBlank()) {
            NuvioShelfSectionHeader(
                title = title,
                modifier = Modifier.padding(horizontal = headerHorizontalPadding),
                onViewAllClick = onViewAllClick,
                viewAllPillSize = viewAllPillSize,
            )
        }
        LazyRow(
            state = state,
            modifier = rowModifier.nuvioDesktopDragScroll(state),
            contentPadding = rowContentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            if (duplicateSafeEntries != null) {
                items(
                    items = duplicateSafeEntries,
                    key = { entry -> entry.lazyKey },
                    contentType = { "poster" },
                ) { keyedEntry ->
                    if (animatePlacement) {
                        Box(modifier = Modifier.animateItem()) { itemContent(keyedEntry.value) }
                    } else {
                        itemContent(keyedEntry.value)
                    }
                }
            } else {
                items(
                    items = entries,
                    contentType = { "poster" },
                ) { entry ->
                    if (animatePlacement) {
                        Box(modifier = Modifier.animateItem()) { itemContent(entry) }
                    } else {
                        itemContent(entry)
                    }
                }
            }
        }
    }
}

internal fun Modifier.nuvioDesktopDragScroll(
    state: LazyListState,
): Modifier {
    if (!isDesktop) return this

    return pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!dragging) {
                    val horizontalDrag =
                        abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                    val verticalDrag =
                        abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                    when {
                        verticalDrag -> break
                        horizontalDrag -> dragging = true
                        else -> continue
                    }
                }

                state.dispatchRawDelta(-delta.x)
                change.consume()
            }
        }
    }
}

internal fun Modifier.nuvioDesktopDragScroll(
    state: ScrollState,
): Modifier {
    if (!isDesktop) return this

    return pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val delta = change.position - change.previousPosition
                totalDx += delta.x
                totalDy += delta.y

                if (!dragging) {
                    val horizontalDrag =
                        abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                    val verticalDrag =
                        abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                    when {
                        verticalDrag -> break
                        horizontalDrag -> dragging = true
                        else -> continue
                    }
                }

                state.dispatchRawDelta(-delta.x)
                change.consume()
            }
        }
    }
}

@Composable
fun NuvioPosterCard(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    basePosterWidthDp: Int? = null,
    shape: NuvioPosterShape = NuvioPosterShape.Poster,
    detailLine: String? = null,
    showTitleBelow: Boolean = true,
    bottomLeftLogoUrl: String? = null,
    bottomLeftText: String? = null,
    anilistScore: Double? = null,
    malScore: Double? = null,
    scoreFormat: AnilistPosterScoreFormat = AnilistPosterScoreFormat.PERCENTAGE,
    anilistStatus: com.nuvio.app.features.anilist.AnilistMediaListStatus? = null,
    anilistProgress: Pair<Int, Int?>? = null,
    anilistUserScore: Double? = null,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val tokens = MaterialTheme.nuvio
    val effectiveBasePosterWidthDp = basePosterWidthDp ?: posterCardStyle.widthDp
    val cardWidth = shape.cardWidth(basePosterWidthDp = effectiveBasePosterWidthDp)
    val cardShape = RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp)
    val shouldShowTitleBelow = showTitleBelow && !posterCardStyle.hideLabelsEnabled

    Column(
        modifier = Modifier
            .desktopPosterHoverScale()
            .then(modifier)
            .width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(shape.aspectRatio)
                .clip(cardShape)
                .background(tokens.colors.surface)
                .nuvioCardDepth(
                    shape = cardShape,
                    surface = NuvioCardDepthSurface.Posters,
                )
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = imageUrl,
                    zoomCornerRadius = posterCardStyle.cornerRadiusDp.dp,
                    hoverScaleEnabled = false,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                NuvioAsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = NuvioTokens.Space.s14),
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.colors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!bottomLeftLogoUrl.isNullOrBlank() || !bottomLeftText.isNullOrBlank()) {
                val maxLogoHeight = when (shape) {
                    NuvioPosterShape.Landscape -> 44.dp
                    NuvioPosterShape.Square -> 56.dp
                    NuvioPosterShape.Poster -> (cardWidth * 0.48f).coerceIn(48.dp, 68.dp)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Transparent,
                                    0.25f to Color.Black.copy(alpha = 0.25f),
                                    0.65f to Color.Black.copy(alpha = 0.65f),
                                    1.00f to Color.Black.copy(alpha = 0.85f),
                                ),
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (!bottomLeftLogoUrl.isNullOrBlank()) {
                        NuvioAsyncImage(
                            model = bottomLeftLogoUrl,
                            contentDescription = stringResource(Res.string.poster_logo_content_description, title),
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .heightIn(min = 20.dp, max = maxLogoHeight),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.BottomCenter,
                        )
                    } else {
                        Text(
                            text = bottomLeftText.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.90f),
                        )
                    }
                }
            }

            PosterRatingBadgesOverlay(
                anilistScore = anilistScore,
                malScore = malScore,
                scoreFormat = scoreFormat,
                modifier = Modifier.align(Alignment.TopEnd),
            )

            if (anilistStatus != null) {
                AnilistPosterStatusBadge(
                    status = anilistStatus,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            if (anilistUserScore != null && anilistUserScore > 0.0) {
                AnilistPosterUserRatingBadge(
                    userScore = anilistUserScore,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }

            NuvioPosterWatchedOverlay(isWatched = isWatched)
        }
        if (shouldShowTitleBelow) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detailLine.isNullOrBlank()) {
                if (detailLine.contains(" • ")) {
                    val parts = detailLine.split(" • ", limit = 2)
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            text = parts[0].trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = parts[1].trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Box(modifier = Modifier.height(NuvioTokens.Space.none))
            }
        } else {
            Box(modifier = Modifier.height(NuvioTokens.Space.none))
        }
    }
}

@Composable
private fun NuvioShelfSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAllClick: (() -> Unit)? = null,
    viewAllPillSize: NuvioViewAllPillSize = NuvioViewAllPillSize.Default,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val viewAllPlaceholderModifier = if (onViewAllClick == null) {
                Modifier
                    .alpha(0f)
                    .clearAndSetSemantics { }
            } else {
                Modifier
            }
            NuvioViewAllPill(
                onClick = onViewAllClick,
                size = viewAllPillSize,
                modifier = viewAllPlaceholderModifier,
            )
        }
    }
}

@Composable
private fun NuvioViewAllPill(
    onClick: (() -> Unit)?,
    size: NuvioViewAllPillSize,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val actionSize = if (size == NuvioViewAllPillSize.Compact) NuvioTokens.Space.s32 else NuvioTokens.Space.s40
    val iconSize = if (size == NuvioViewAllPillSize.Compact) NuvioTokens.Icon.sm else tokens.icons.md
    val viewAllText = stringResource(Res.string.home_view_all)

    Box(
        modifier = modifier
            .size(actionSize)
            .background(
                color = tokens.colors.surface,
                shape = RoundedCornerShape(NuvioTokens.Radius.xl),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = viewAllText,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(iconSize),
        )
    }
}

internal const val NuvioDesktopCatalogShelfPosterScale = 1.4f
private const val DesktopPosterHoverScale = 1.045f

internal fun desktopCatalogShelfPosterBaseWidthDp(
    basePosterWidthDp: Int,
): Int =
    if (isDesktop) {
        (basePosterWidthDp * NuvioDesktopCatalogShelfPosterScale).roundToInt()
    } else {
        basePosterWidthDp
    }

internal fun Modifier.nuvioShelfHoverOverdraw(inset: Dp): Modifier {
    if (inset == 0.dp) return this

    // Expand the measured viewport, then place it negatively so edge items keep
    // their visual alignment while desktop hover scale can draw into the gutter.
    return layout { measurable, constraints ->
        val insetPx = inset.roundToPx()
        val horizontalInset = insetPx * 2
        val verticalInset = insetPx * 2
        val expandedMaxWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth + horizontalInset
        } else {
            constraints.maxWidth
        }
        val expandedMinWidth = if (constraints.hasBoundedWidth) {
            (constraints.minWidth + horizontalInset).coerceAtMost(expandedMaxWidth)
        } else {
            constraints.minWidth
        }
        val expandedMaxHeight = if (constraints.hasBoundedHeight) {
            constraints.maxHeight + verticalInset
        } else {
            constraints.maxHeight
        }
        val expandedConstraints = constraints.copy(
            minWidth = expandedMinWidth,
            maxWidth = expandedMaxWidth,
            minHeight = 0,
            maxHeight = expandedMaxHeight,
        )
        val placeable = measurable.measure(expandedConstraints)
        val width = (placeable.width - horizontalInset)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height - verticalInset)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            placeable.placeRelative(-insetPx, -insetPx)
        }
    }
}

private val NuvioPosterShape.aspectRatio: Float
    get() = when (this) {
        NuvioPosterShape.Poster -> 0.675f
        NuvioPosterShape.Square -> 1f
        NuvioPosterShape.Landscape -> PosterLandscapeAspectRatio
    }

private data class CatalogLogoOverlaySize(
    val width: Dp,
    val height: Dp,
    val textMaxWidth: Dp,
)

private fun catalogLogoOverlaySize(
    basePosterWidthDp: Int,
    shape: NuvioPosterShape,
): CatalogLogoOverlaySize =
    if (shape == NuvioPosterShape.Landscape) {
        when {
            basePosterWidthDp <= 108 -> CatalogLogoOverlaySize(width = 92.dp, height = 24.dp, textMaxWidth = 120.dp)
            basePosterWidthDp <= 120 -> CatalogLogoOverlaySize(width = 104.dp, height = 28.dp, textMaxWidth = 132.dp)
            basePosterWidthDp <= 132 -> CatalogLogoOverlaySize(width = 116.dp, height = 30.dp, textMaxWidth = 144.dp)
            else -> CatalogLogoOverlaySize(width = 128.dp, height = 34.dp, textMaxWidth = 156.dp)
        }
    } else {
        when {
            basePosterWidthDp <= 108 -> CatalogLogoOverlaySize(width = 72.dp, height = 18.dp, textMaxWidth = 92.dp)
            basePosterWidthDp <= 120 -> CatalogLogoOverlaySize(width = 80.dp, height = 20.dp, textMaxWidth = 104.dp)
            basePosterWidthDp <= 132 -> CatalogLogoOverlaySize(width = 88.dp, height = 22.dp, textMaxWidth = 112.dp)
            else -> CatalogLogoOverlaySize(width = 96.dp, height = 24.dp, textMaxWidth = 124.dp)
        }
    }

internal fun NuvioPosterShape.cardWidth(basePosterWidthDp: Int): Dp =
    when (this) {
        NuvioPosterShape.Poster -> basePosterWidthDp.dp
        NuvioPosterShape.Square -> basePosterWidthDp.dp
        NuvioPosterShape.Landscape -> landscapePosterWidth(basePosterWidthDp)
    }

@Composable
internal fun Modifier.desktopPosterHoverScale(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    if (!enabled || !isDesktop) return this

    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (hovered) DesktopPosterHoverScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "desktop_poster_hover_scale",
    )

    val isScaling = hovered || scale != 1f

    return this
        .then(
            if (isScaling) {
                Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .zIndex(1f)
            } else {
                Modifier
            },
        )
        .hoverable(interactionSource)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.posterCardClickable(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    zoomImageUrl: String? = null,
    zoomCornerRadius: Dp = NuvioTokens.Radius.poster,
    hoverScaleEnabled: Boolean = true,
): Modifier {
    if (onClick == null && onLongClick == null) return this
    val bounds = remember { mutableStateOf<Rect?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val handleLongClick = onLongClick?.let { longClick ->
        {
            bounds.value?.takeIf { zoomImageUrl != null }?.let { cardBounds ->
                PosterZoomAnchorHolder.stash(
                    PosterZoomAnchor(
                        boundsInRoot = cardBounds,
                        imageUrl = zoomImageUrl,
                        cornerRadius = zoomCornerRadius,
                    ),
                )
            }
            longClick()
        }
    }
    return this
        .onGloballyPositioned { coordinates -> bounds.value = coordinates.unclippedBoundsInRoot() }
        .desktopPosterHoverScale(
            enabled = hoverScaleEnabled,
            interactionSource = interactionSource,
        )
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick?.invoke() },
            onLongClick = handleLongClick,
        )
        .secondaryClick(handleLongClick)
}

private fun androidx.compose.ui.layout.LayoutCoordinates.unclippedBoundsInRoot(): Rect {
    val position = positionInRoot()
    return Rect(
        left = position.x,
        top = position.y,
        right = position.x + size.width,
        bottom = position.y + size.height,
    )
}

fun formatAnilistScore(
    score: Double?,
    format: AnilistPosterScoreFormat = AnilistPosterScoreFormat.PERCENTAGE,
): String? {
    if (score == null || score <= 0.0) return null
    return when (format) {
        AnilistPosterScoreFormat.PERCENTAGE -> {
            val pct = if (score <= 10.0) (score * 10.0).roundToInt() else score.roundToInt()
            "$pct%"
        }
        AnilistPosterScoreFormat.POINT_10 -> {
            val score10 = if (score > 10.0) (score / 10.0) else score
            val rounded = (score10 * 10.0).roundToInt()
            val whole = rounded / 10
            val decimal = (rounded % 10).absoluteValue
            "$whole.$decimal"
        }
    }
}

@Composable
private fun PosterRatingBadgesOverlay(
    anilistScore: Double?,
    malScore: Double?,
    scoreFormat: AnilistPosterScoreFormat = AnilistPosterScoreFormat.PERCENTAGE,
    modifier: Modifier = Modifier,
) {
    val hasAnilist = anilistScore != null && anilistScore > 0
    val hasMal = malScore != null && malScore > 0
    if (!hasAnilist && !hasMal) return

    Row(
        modifier = modifier
            .padding(top = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasAnilist) {
            val formatted = formatAnilistScore(anilistScore, scoreFormat)
            if (formatted != null) {
                PosterScoreBadge(
                    logo = Res.drawable.rating_anilist,
                    contentDescription = "AniList",
                    text = formatted,
                )
            }
        }
        if (hasMal) {
            val roundedMal = (malScore!! * 100.0).roundToInt()
            val whole = roundedMal / 100
            val decimal = (roundedMal % 100).absoluteValue
            val malFormatted = "$whole.${decimal.toString().padStart(2, '0')}"
            PosterScoreBadge(
                logo = Res.drawable.rating_mal,
                contentDescription = "MyAnimeList",
                text = malFormatted,
            )
        }
    }
}

@Composable
private fun PosterScoreBadge(
    logo: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Black.copy(alpha = 0.72f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Image(
                painter = painterResource(logo),
                contentDescription = contentDescription,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

private val AnilistClockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AnilistClock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(11.99f, 2f)
            curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveTo(6.47f, 22f, 11.99f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
            close()
            moveTo(12f, 20f)
            curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
            reflectiveCurveTo(7.58f, 4f, 12f, 4f)
            reflectiveCurveTo(20f, 7.58f, 20f, 12f)
            reflectiveCurveTo(16.42f, 20f, 12f, 20f)
            close()
            moveTo(12.5f, 7f)
            horizontalLineTo(11f)
            verticalLineTo(13f)
            lineTo(16.25f, 16.15f)
            lineTo(17f, 14.92f)
            lineTo(12.5f, 12.25f)
            verticalLineTo(7f)
            close()
        }
    }.build()
}

private val AnilistPauseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AnilistPause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6f, 19f)
            horizontalLineTo(10f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(14f, 5f)
            verticalLineTo(19f)
            horizontalLineTo(18f)
            verticalLineTo(5f)
            horizontalLineTo(14f)
            close()
        }
    }.build()
}

private val AnilistRepeatIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AnilistRepeat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7f, 7f)
            horizontalLineTo(17f)
            verticalLineTo(10f)
            lineTo(21f, 6f)
            lineTo(17f, 2f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(11f)
            horizontalLineTo(7f)
            verticalLineTo(7f)
            close()
            moveTo(17f, 17f)
            horizontalLineTo(7f)
            verticalLineTo(14f)
            lineTo(3f, 18f)
            lineTo(7f, 22f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            horizontalLineTo(17f)
            verticalLineTo(17f)
            close()
        }
    }.build()
}

@Composable
fun AnilistPosterStatusBadge(
    status: com.nuvio.app.features.anilist.AnilistMediaListStatus,
    modifier: Modifier = Modifier,
) {
    val (iconVector, bgTint, iconTint) = when (status) {
        com.nuvio.app.features.anilist.AnilistMediaListStatus.CURRENT -> Triple(
            Icons.Default.PlayArrow,
            Color(0xFF4CAF50), // Vibrant Green (AniHyou style)
            Color.White,
        )
        com.nuvio.app.features.anilist.AnilistMediaListStatus.PLANNING -> Triple(
            AnilistClockIcon,
            Color(0xCC2A2A2A), // Dark Translucent Charcoal (AniHyou style)
            Color.White,
        )
        com.nuvio.app.features.anilist.AnilistMediaListStatus.COMPLETED -> Triple(
            Icons.Default.Check,
            Color(0xFF90CAF9), // Soft Sky Blue (AniHyou style)
            Color(0xFF0D47A1), // Deep Navy
        )
        com.nuvio.app.features.anilist.AnilistMediaListStatus.PAUSED -> Triple(
            AnilistPauseIcon,
            Color(0xFFE2D84C), // Mustard / Yellow (AniHyou style)
            Color(0xFF1E1E1E), // Dark Charcoal
        )
        com.nuvio.app.features.anilist.AnilistMediaListStatus.DROPPED -> Triple(
            Icons.Default.Close,
            Color(0xFFE53935), // Red
            Color.White,
        )
        com.nuvio.app.features.anilist.AnilistMediaListStatus.REPEATING -> Triple(
            AnilistRepeatIcon,
            Color(0xFFAB47BC), // Purple
            Color.White,
        )
    }

    Surface(
        shape = RoundedCornerShape(bottomEnd = 8.dp),
        color = bgTint,
        modifier = modifier.size(24.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = status.name,
                tint = iconTint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun AnilistPosterUserRatingBadge(
    userScore: Double?,
    modifier: Modifier = Modifier,
) {
    if (userScore == null || userScore <= 0.0) return

    // Normalize to 1-10 scale (AniList scores can be 0-100 or 0-10)
    val score10 = if (userScore > 10.0) userScore / 10.0 else userScore

    // AniHyou dynamic color tiers based on score
    val (bgTint, textTint) = when {
        score10 >= 7.5 -> Color(0xFF4DD0E1) to Color(0xFF003840) // Cyan / Teal for high scores (7.5 - 10)
        score10 >= 6.0 -> Color(0xFFAED581) to Color(0xFF1B3A00) // Soft Green for good scores (6.0 - 7.4)
        score10 >= 4.0 -> Color(0xFFFFB74D) to Color(0xFF3E2000) // Warm Amber for average (4.0 - 5.9)
        else -> Color(0xFFEF5350) to Color(0xFFFFFFFF)           // Coral Red for low scores (< 4.0)
    }

    val roundedScore = ((score10 * 10.0).roundToInt() / 10.0)
    val formattedScore = if (roundedScore % 1.0 == 0.0) {
        roundedScore.toInt().toString()
    } else {
        roundedScore.toString()
    }

    Surface(
        shape = RoundedCornerShape(topEnd = 8.dp),
        color = bgTint,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "User Rating",
                tint = textTint,
                modifier = Modifier.size(10.dp),
            )
            Text(
                text = formattedScore,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                ),
                color = textTint,
                maxLines = 1,
            )
        }
    }
}
