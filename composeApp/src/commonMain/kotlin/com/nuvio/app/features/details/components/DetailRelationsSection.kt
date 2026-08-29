package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.features.details.MetaRelation

@Composable
fun DetailRelationsSection(
    relations: List<MetaRelation>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
    onRelationClick: ((MetaRelation) -> Unit)? = null,
) {
    if (relations.isEmpty()) return

    DetailSection(
        title = "Franchise & Relations",
        modifier = modifier,
        showHeader = showHeader,
    ) {
        val rowState = rememberLazyListState()
        LazyRow(
            state = rowState,
            modifier = Modifier
                .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                .fillMaxWidth()
                .nuvioDesktopDragScroll(rowState),
            contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = relations,
                key = { it.id },
            ) { relation ->
                RelationCard(
                    relation = relation,
                    onClick = onRelationClick?.let { { it(relation) } },
                )
            }
        }
    }
}

@Composable
private fun RelationCard(
    relation: MetaRelation,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .width(135.dp)
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .nuvioCardDepth(
                    shape = cardShape,
                    surface = NuvioCardDepthSurface.Posters,
                ),
        ) {
            if (!relation.poster.isNullOrBlank()) {
                AsyncImage(
                    model = relation.poster,
                    contentDescription = relation.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f),
                            ),
                            startY = 100f,
                        )
                    )
            )

            // Relation Type Badge (Prequel, Sequel, Side Story, etc.)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (relation.relationType.uppercase()) {
                            "PREQUEL" -> MaterialTheme.colorScheme.tertiary
                            "SEQUEL" -> MaterialTheme.colorScheme.primary
                            "MOVIE" -> Color(0xFFE50914)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
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
                        "MOVIE" -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }

            // Format & Episode count at bottom
            val metaSubtitle = listOfNotNull(
                relation.format?.takeIf { it.isNotBlank() },
                relation.episodes?.takeIf { it > 0 }?.let { "$it eps" },
            ).joinToString(" • ")

            if (metaSubtitle.isNotBlank()) {
                Text(
                    text = metaSubtitle,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = relation.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
