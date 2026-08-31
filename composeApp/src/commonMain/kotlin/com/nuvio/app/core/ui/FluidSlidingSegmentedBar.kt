package com.nuvio.app.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class FluidTabBounds(val x: Dp, val width: Dp)

/**
 * A fluid, physics-based segmented tab bar that animates a sliding capsule indicator
 * smoothly between tabs with a spring physics transition.
 */
@Composable
fun <T> FluidSlidingSegmentedBar(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    cornerRadius: Dp = 24.dp,
    spacing: Dp = 4.dp,
    padding: Dp = 4.dp,
    tabContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val tabBounds = remember { mutableStateMapOf<T, FluidTabBounds>() }

    val currentBounds = tabBounds[selectedItem]
    val animatedOffsetX by animateDpAsState(
        targetValue = currentBounds?.x ?: 0.dp,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "FluidSlidingIndicatorOffset",
    )
    val animatedWidth by animateDpAsState(
        targetValue = currentBounds?.width ?: 0.dp,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "FluidSlidingIndicatorWidth",
    )

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(padding)
                .height(IntrinsicSize.Min),
        ) {
            // Fluid sliding indicator capsule in the background
            if (animatedWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .width(animatedWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(cornerRadius - padding))
                        .background(indicatorColor),
                )
            }

            // Tab items row on top
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = item == selectedItem
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .onPlaced { layoutCoordinates ->
                                val x = with(density) { layoutCoordinates.positionInParent().x.toDp() }
                                val width = with(density) { layoutCoordinates.size.width.toDp() }
                                tabBounds[item] = FluidTabBounds(x, width)
                            }
                            .clip(RoundedCornerShape(cornerRadius - padding))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (item != selectedItem) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onItemSelected(item)
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        tabContent(item, isSelected)
                    }
                }
            }
        }
    }
}
