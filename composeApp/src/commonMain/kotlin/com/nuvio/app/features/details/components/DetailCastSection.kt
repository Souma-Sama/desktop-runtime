package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.castAvatarSharedTransitionKey
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun DetailCastSection(
    cast: List<MetaPerson>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
    onCastClick: ((MetaPerson, String?) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    if (cast.isEmpty()) return

    val categories = remember(cast) {
        val distinctCats = cast.mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }.distinct()
        if (distinctCats.size > 1) {
            distinctCats
        } else {
            emptyList()
        }
    }

    var selectedCategory by remember(cast) { mutableStateOf(categories.firstOrNull().orEmpty()) }

    val filteredCast = remember(cast, selectedCategory) {
        if (categories.isEmpty() || selectedCategory.isBlank()) {
            cast
        } else {
            cast.filter { it.category == selectedCategory }
        }
    }

    DetailSection(
        title = stringResource(Res.string.settings_meta_cast),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (categories.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .nuvioDesktopDragScroll(scrollState)
                        .padding(horizontal = horizontalScrollPadding),
                ) {
                    com.nuvio.app.core.ui.FluidSlidingSegmentedBar(
                        items = categories,
                        selectedItem = selectedCategory,
                        onItemSelected = { selectedCategory = it },
                    ) { cat, isSelected ->
                        val count = remember(cast, cat) { cast.count { it.category == cat } }
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                ),
                                color = contentColor,
                                maxLines = 1,
                            )
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = if (count > 999) "${count / 1000}k" else "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        ),
                                        color = contentColor,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BoxWithConstraints {
                val sizing = castSectionSizing(maxWidth.value)
                val rowState = rememberLazyListState()
                LaunchedEffect(selectedCategory) {
                    runCatching { rowState.scrollToItem(0) }
                }

                LazyRow(
                    state = rowState,
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth()
                        .nuvioDesktopDragScroll(rowState),
                    contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                    horizontalArrangement = Arrangement.spacedBy(sizing.avatarGap),
                ) {
                    itemsIndexed(
                        items = filteredCast,
                        key = { index, person -> "${person.name}-${person.role.orEmpty()}-${person.photo.orEmpty()}-$index" },
                    ) { index, person ->
                        val sharedTransitionKey = (person.tmdbId ?: (person.name.hashCode() and 0x7FFFFFFF))
                            .takeIf { it > 0 }
                            ?.let { castAvatarSharedTransitionKey(it, occurrenceIndex = index) }
                        CastItem(
                            person = person,
                            sharedTransitionKey = sharedTransitionKey,
                            sizing = sizing,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = if (onCastClick != null && person.name.isNotBlank()) {
                                { onCastClick(person, sharedTransitionKey) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun CastItem(
    person: MetaPerson,
    modifier: Modifier = Modifier,
    sharedTransitionKey: String? = null,
    sizing: CastSectionSizing,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onClick: (() -> Unit)? = null,
) {
    val avatarCacheKey = sharedTransitionKey
    val platformContext = LocalPlatformContext.current
    val avatarRequest = if (!person.photo.isNullOrBlank() && !avatarCacheKey.isNullOrBlank()) {
        remember(platformContext, person.photo, avatarCacheKey) {
            ImageRequest.Builder(platformContext)
                .data(person.photo)
                .memoryCacheKey(avatarCacheKey)
                .placeholderMemoryCacheKey(avatarCacheKey)
                .diskCacheKey(person.photo)
                .build()
        }
    } else {
        null
    }

    val avatarSharedElementModifier = if (
        sharedTransitionScope != null &&
            animatedVisibilityScope != null &&
            !sharedTransitionKey.isNullOrBlank()
    ) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(
                    key = sharedTransitionKey,
                ),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }
    val clickInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .width(sizing.itemWidth)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = clickInteractionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .then(avatarSharedElementModifier)
                .size(sizing.avatarSize)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                )
                .nuvioCardDepth(
                    shape = CircleShape,
                    surface = NuvioCardDepthSurface.Cast,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (person.photo != null) {
                AsyncImage(
                    model = avatarRequest ?: person.photo,
                    contentDescription = person.name,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = person.name.initials(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = person.name,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = sizing.nameLabelSize,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!person.role.isNullOrBlank()) {
            Text(
                text = person.role,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = sizing.subLabelSize,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class CastSectionSizing(
    val avatarSize: androidx.compose.ui.unit.Dp,
    val itemWidth: androidx.compose.ui.unit.Dp,
    val avatarGap: androidx.compose.ui.unit.Dp,
    val nameLabelSize: TextUnit,
    val subLabelSize: TextUnit,
)

private fun castSectionSizing(maxWidthDp: Float): CastSectionSizing =
    when {
        maxWidthDp >= 1200f -> CastSectionSizing(
            avatarSize = 100.dp,
            itemWidth = 112.dp,
            avatarGap = 20.dp,
            nameLabelSize = 16.sp,
            subLabelSize = 14.sp,
        )
        maxWidthDp >= 840f -> CastSectionSizing(
            avatarSize = 90.dp,
            itemWidth = 102.dp,
            avatarGap = 18.dp,
            nameLabelSize = 15.sp,
            subLabelSize = 13.sp,
        )
        maxWidthDp >= 600f -> CastSectionSizing(
            avatarSize = 85.dp,
            itemWidth = 98.dp,
            avatarGap = 16.dp,
            nameLabelSize = 14.sp,
            subLabelSize = 12.sp,
        )
        else -> CastSectionSizing(
            avatarSize = 80.dp,
            itemWidth = 92.dp,
            avatarGap = 16.dp,
            nameLabelSize = 14.sp,
            subLabelSize = 12.sp,
        )
    }

private fun String.initials(): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> ""
    }
}
