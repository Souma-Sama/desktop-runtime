package com.nuvio.app.features.anilist.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.details.components.ShapePill
import com.nuvio.app.features.details.components.ShapeTile
import com.nuvio.app.features.details.components.TrackerGlassCard

@Composable
fun AniHyouPositionalStatItemView(
    name: String,
    position: Int,
    count: Int,
    meanScore: Double,
    modifier: Modifier = Modifier,
    minutesWatched: Long? = null,
    chaptersRead: Int? = null,
    imageUrl: String? = null,
    onClick: () -> Unit = {},
) {
    TrackerGlassCard(
        modifier = modifier,
        shape = ShapeTile,
        onClick = onClick,
    ) {
        // Top Row: Avatar (if any) + Name + Position badge (#1, #2...)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(ShapePill)
                        .border(1.dp, Color.White.copy(alpha = 0.20f), ShapePill),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(ShapePill)
                    .background(Color(0xFF00A2FF).copy(alpha = 0.20f))
                    .border(1.dp, Color(0xFF00A2FF).copy(alpha = 0.60f), ShapePill)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$position",
                    color = Color(0xFF00A2FF),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Row: Title Count | Mean Score | Time Spent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatMetricColumn(
                value = count.toString(),
                label = "Titles",
                valueColor = Color.White,
            )
            StatMetricColumn(
                value = if (meanScore > 0.0) "${meanScore}%" else "—",
                label = "Mean score",
                valueColor = if (meanScore >= 75.0) Color(0xFF10B981) else Color(0xFFFFB800),
            )
            val timeStr = when {
                minutesWatched != null && minutesWatched > 0L -> {
                    val hours = minutesWatched / 60L
                    if (hours >= 24) {
                        val days = (hours / 24.0 * 10.0).toInt() / 10.0
                        "${days}d"
                    } else {
                        "${hours}h"
                    }
                }
                chaptersRead != null && chaptersRead > 0 -> "${chaptersRead} ch"
                else -> "—"
            }
            StatMetricColumn(
                value = timeStr,
                label = "Time spent",
                valueColor = Color(0xFF38BDF8),
            )
        }
    }
}

@Composable
private fun StatMetricColumn(
    value: String,
    label: String,
    valueColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}
