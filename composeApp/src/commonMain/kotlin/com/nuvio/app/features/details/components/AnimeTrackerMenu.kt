package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistMediaListStatus
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import kotlinx.coroutines.launch

@Composable
fun AnimeTrackerButton(
    modifier: Modifier = Modifier,
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    title: String? = null,
    size: androidx.compose.ui.unit.Dp = 52.dp,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val trackerState by AnilistTrackerCoordinator.trackerState.collectAsState()
    val isTrackingActive = trackerState.entry?.status != null
    val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: meta?.name.orEmpty()

    LaunchedEffect(meta?.id, effectiveTitle) {
        if (effectiveTitle.isNotBlank() || meta != null) {
            val metaYear = meta?.releaseInfo?.take(4)?.toIntOrNull()
            AnilistTrackerCoordinator.loadForMedia(
                title = effectiveTitle,
                mediaId = meta?.id,
                year = metaYear,
                genres = meta?.genres.orEmpty(),
                country = meta?.country,
                language = meta?.language,
            )
        }
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = if (isTrackingActive) {
                MaterialTheme.colorScheme.primary
            } else if (menuExpanded) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
            },
            contentColor = if (isTrackingActive) {
                MaterialTheme.colorScheme.onPrimary
            } else if (menuExpanded) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            tonalElevation = 6.dp,
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clickable(role = Role.Button) {
                        menuExpanded = !menuExpanded
                        if (menuExpanded && trackerState.media == null) {
                            val metaYear = meta?.releaseInfo?.take(4)?.toIntOrNull()
                            AnilistTrackerCoordinator.loadForMedia(
                                title = effectiveTitle,
                                mediaId = meta?.id,
                                year = metaYear,
                                genres = meta?.genres.orEmpty(),
                                country = meta?.country,
                                language = meta?.language,
                                forceRefresh = true,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (trackerState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = com.nuvio.app.features.anilist.AnilistLogoVector,
                        contentDescription = "AniList Tracker",
                        modifier = Modifier.size(20.dp),
                        tint = if (isTrackingActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            com.nuvio.app.features.anilist.AnilistBrandBlue
                        },
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(8.dp),
        ) {
            AnimeTrackerDropdownContent(
                meta = meta,
                title = effectiveTitle,
                onClose = { menuExpanded = false },
            )
        }
    }
}

@Composable
fun AnimeTrackerDropdownContent(
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    title: String? = null,
    onClose: () -> Unit,
) {
    val trackerState by AnilistTrackerCoordinator.trackerState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var tokenInput by remember { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }
    var manualSearchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        // Header with Logo & User Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = com.nuvio.app.features.anilist.AnilistLogoVector,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = com.nuvio.app.features.anilist.AnilistBrandBlue,
                )
                Text(
                    text = "AniList",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (trackerState.isAuthenticated) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = trackerState.user?.name ?: "Connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(
                        onClick = { AnilistAuthRepository.logout() },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Disconnect",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )

        val uriHandler = LocalUriHandler.current

        // Login prompt if unauthenticated
        if (!trackerState.isAuthenticated) {
            Text(
                text = "Connect your AniList account to track episodes and update your anime list.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (!showTokenInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            uriHandler.openUri(AnilistAuthRepository.OAUTH_AUTHORIZE_URL)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Authorize", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showTokenInput = true },
                    ) {
                        Text("Paste Token", fontSize = 12.sp)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        placeholder = { Text("Paste AniList Token / Pin URL...", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (tokenInput.isNotBlank()) {
                                coroutineScope.launch {
                                    val token = tokenInput.substringAfter("access_token=").substringBefore("&").trim()
                                    AnilistAuthRepository.loginWithToken(token)
                                    showTokenInput = false
                                }
                            }
                        },
                        enabled = tokenInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        if (trackerState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            return@Column
        }

        if (trackerState.media == null) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = trackerState.error ?: "No matching anime found on AniList.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val effectiveRetryTitle = title?.takeIf { it.isNotBlank() } ?: meta?.name.orEmpty()
                    if (effectiveRetryTitle.isNotBlank() || meta != null) {
                        Button(
                            onClick = {
                                val metaYear = meta?.releaseInfo?.take(4)?.toIntOrNull()
                                AnilistTrackerCoordinator.loadForMedia(
                                    title = effectiveRetryTitle,
                                    mediaId = meta?.id,
                                    year = metaYear,
                                    genres = meta?.genres.orEmpty(),
                                    country = meta?.country,
                                    language = meta?.language,
                                    forceRefresh = true,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualSearchText,
                    onValueChange = { manualSearchText = it },
                    placeholder = { Text("Search title or AniList ID (e.g. 140960)...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (manualSearchText.isNotBlank()) {
                            val query = manualSearchText.trim()
                            val numId = query.toIntOrNull()
                            if (numId != null) {
                                AnilistTrackerCoordinator.loadForMedia(
                                    title = query,
                                    mediaId = "anilist:$numId",
                                    forceRefresh = true,
                                )
                            } else {
                                AnilistTrackerCoordinator.loadForMedia(
                                    title = query,
                                    forceRefresh = true,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualSearchText.isNotBlank(),
                ) {
                    Text("Search AniList", fontSize = 12.sp)
                }
            }
            return@Column
        }

        // Matched Anime Title
        Text(
            text = trackerState.media?.title?.displayTitle.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Watch Status Section
        Text(
            text = "Watch Status",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))

        val currentStatus = trackerState.entry?.status
        val statuses = listOf(
            AnilistMediaListStatus.CURRENT to Icons.Default.PlayArrow,
            AnilistMediaListStatus.PLANNING to Icons.Default.BookmarkBorder,
            AnilistMediaListStatus.COMPLETED to Icons.Default.CheckCircle,
            AnilistMediaListStatus.PAUSED to Icons.Default.Remove,
            AnilistMediaListStatus.DROPPED to Icons.Default.Close,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            statuses.forEach { (status, icon) ->
                val isSelected = currentStatus == status
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            AnilistTrackerCoordinator.updateStatus(status)
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(10.dp))

        // Episode Progress Stepper
        val episodesTotal = trackerState.media?.episodes
        val currentProgress = trackerState.entry?.progress ?: 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    IconButton(
                        onClick = { AnilistTrackerCoordinator.decrementProgress() },
                        enabled = currentProgress > 0,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Episode",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                Text(
                    text = if (episodesTotal != null) {
                        "Ep $currentProgress / $episodesTotal"
                    } else {
                        "Ep $currentProgress"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    IconButton(
                        onClick = { AnilistTrackerCoordinator.incrementProgress() },
                        enabled = episodesTotal == null || currentProgress < episodesTotal,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Episode",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rating / Score Section (1-10)
        val currentScore = trackerState.entry?.score ?: 0.0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Score",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = if (currentScore > 0.0) "${currentScore.toInt()} / 10" else "Unrated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..10).forEach { starIndex ->
                val isFilled = starIndex <= currentScore.toInt()
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            val newScore = if (currentScore.toInt() == starIndex) 0.0 else starIndex.toDouble()
                            AnilistTrackerCoordinator.updateScore(newScore)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Rate $starIndex",
                        modifier = Modifier.size(16.dp),
                        tint = if (isFilled) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }

        if (trackerState.entry != null) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = { AnilistTrackerCoordinator.deleteEntry() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remove from AniList",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
