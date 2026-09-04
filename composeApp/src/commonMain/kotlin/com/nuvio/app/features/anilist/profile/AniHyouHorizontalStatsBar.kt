package com.nuvio.app.features.anilist.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvioHorizontalScroll

data class HorizontalStatItem(
    val label: String,
    val value: Float,
    val color: Color,
    val onColor: Color = Color.Black,
)

@Composable
fun AniHyouHorizontalStatsBar(
    stats: List<HorizontalStatItem>,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 8.dp,
    showTotal: Boolean = true,
) {
    val nonZeroStats = remember(stats) { stats.filter { it.value > 0f } }
    if (nonZeroStats.isEmpty()) return
    val totalValue = remember(nonZeroStats) { nonZeroStats.sumOf { it.value.toDouble() }.toFloat() }
    val chipsScrollState = rememberScrollState()

    Column(
        modifier = modifier.padding(vertical = verticalPadding),
    ) {
        // Horizontally scrolling chips
        Row(
            modifier = Modifier
                .nuvioHorizontalScroll(chipsScrollState)
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            nonZeroStats.forEach { stat ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stat.color,
                    contentColor = stat.onColor,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stat.value.toInt().toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = stat.onColor,
                        )
                        Text(
                            text = stat.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = stat.onColor,
                        )
                    }
                }
            }
        }

        // Multi-segmented stacked progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 10.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            nonZeroStats.forEach { stat ->
                Box(
                    modifier = Modifier
                        .weight(stat.value)
                        .height(20.dp)
                        .background(stat.color),
                )
            }
        }

        if (showTotal) {
            Text(
                text = "Total entries: ${totalValue.toInt()}",
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
