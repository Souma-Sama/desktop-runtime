package com.nuvio.app.features.anilist.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistBrandBlue
import com.nuvio.app.features.anilist.AnilistLogoVector
import com.nuvio.app.features.anilist.AnilistPreferencesRepository

/**
 * A macOS-style floating notification banner that appears in the top-right corner
 * when an active AniList API outage is detected.
 *
 * Strictly adheres to Rule 17 (Zero Spillover) by only composing when AniList is enabled.
 */
@Composable
fun AnilistOutageNotification(
    modifier: Modifier = Modifier,
) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    if (!prefs.enabled) return

    val outageMessage by AnilistApi.outageMessage.collectAsStateWithLifecycle()
    var isDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(outageMessage) {
        if (outageMessage == null) {
            isDismissed = false
        }
    }

    val isVisible = !isDismissed && !outageMessage.isNullOrBlank()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        val messageText = outageMessage.orEmpty()

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xEC1E1E22),
            border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                )
                .widthIn(max = 380.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Left: App Icon badge with outage status dot
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1F2B3E), Color(0xFF101924))
                                )
                            )
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AnilistLogoVector,
                            contentDescription = "AniList",
                            tint = AnilistBrandBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    // Red status indicator dot at bottom corner
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4D4D))
                            .border(1.5.dp, Color(0xFF1E1E22), CircleShape)
                    )
                }

                // Middle: Typography
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "ANILIST SERVICE ALERT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                fontSize = 10.5.sp,
                            ),
                            color = Color(0xFFFF6B6B),
                        )
                        Text(
                            text = "now",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }

                    Text(
                        text = "API Temporarily Disabled",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Close button (x)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isDismissed = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}
