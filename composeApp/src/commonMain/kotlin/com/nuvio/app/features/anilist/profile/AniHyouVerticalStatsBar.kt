package com.nuvio.app.features.anilist.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvioHorizontalScroll

const val MAX_VERTICAL_STAT_HEIGHT = 124

data class VerticalStatItem(
    val label: String,
    val value: Float,
    val displayValue: String? = null,
    val color: Color? = null,
)

@Composable
fun AniHyouVerticalStatsBar(
    stats: List<VerticalStatItem>,
    modifier: Modifier = Modifier,
    mapColorTo: @Composable (VerticalStatItem) -> Color = { it.color ?: MaterialTheme.colorScheme.primary },
) {
    if (stats.isEmpty()) return
    val maxValue = remember(stats) { stats.maxOfOrNull { it.value } ?: 0f }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .nuvioHorizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.Bottom,
    ) {
        stats.forEach { stat ->
            val barHeight = if (maxValue > 0f) {
                ((stat.value / maxValue) * MAX_VERTICAL_STAT_HEIGHT).coerceAtLeast(4f).dp
            } else {
                4.dp
            }
            val barColor = mapColorTo(stat)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                Text(
                    text = stat.displayValue ?: (if (stat.value > 0) stat.value.toInt().toString() else ""),
                    modifier = Modifier.padding(bottom = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Canvas(modifier = Modifier.size(width = 25.dp, height = barHeight)) {
                    drawRoundRect(
                        color = barColor,
                        size = size,
                        cornerRadius = CornerRadius(x = 16f, y = 16f),
                    )
                }
                Text(
                    text = stat.label,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
