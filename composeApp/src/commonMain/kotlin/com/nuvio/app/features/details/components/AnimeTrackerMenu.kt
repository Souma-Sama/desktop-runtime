package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistFuzzyDate
import com.nuvio.app.features.anilist.AnilistMediaListStatus
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.rating_anilist
import org.jetbrains.compose.resources.painterResource

// Status Color Tokens
private val StatusColorWatching = Color(0xFF00A2FF)
private val StatusColorPlanning = Color(0xFF8B5CF6)
private val StatusColorCompleted = Color(0xFF10B981)
private val StatusColorOnHold = Color(0xFFF59E0B)
private val StatusColorDropped = Color(0xFFEF4444)
private val StatusColorRepeating = Color(0xFF06B6D4)

fun getStatusColor(status: AnilistMediaListStatus?): Color = when (status) {
    AnilistMediaListStatus.CURRENT -> StatusColorWatching
    AnilistMediaListStatus.PLANNING -> StatusColorPlanning
    AnilistMediaListStatus.COMPLETED -> StatusColorCompleted
    AnilistMediaListStatus.PAUSED -> StatusColorOnHold
    AnilistMediaListStatus.DROPPED -> StatusColorDropped
    AnilistMediaListStatus.REPEATING -> StatusColorRepeating
    null -> Color(0xFF00A2FF)
}

@Composable
fun AnimeTrackerButton(
    modifier: Modifier = Modifier,
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    title: String? = null,
    size: Dp = 52.dp,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val trackerState by AnilistTrackerCoordinator.trackerState.collectAsState()
    val isTrackingActive = trackerState.entry?.status != null
    val currentStatus = trackerState.entry?.status
    val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: meta?.name.orEmpty()
    val hapticFeedback = LocalHapticFeedback.current

    val statusColor = getStatusColor(currentStatus)

    val containerColor by animateColorAsState(
        targetValue = when {
            isTrackingActive -> statusColor.copy(alpha = 0.16f)
            menuExpanded -> MaterialTheme.colorScheme.onBackground
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        },
        animationSpec = tween(250),
        label = "TrackerButtonBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isTrackingActive -> statusColor.copy(alpha = 0.65f)
            menuExpanded -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        },
        animationSpec = tween(250),
        label = "TrackerButtonBorder",
    )

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = if (isTrackingActive || menuExpanded) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape,
                ),
            shape = CircleShape,
            color = containerColor,
            tonalElevation = if (isTrackingActive) 8.dp else 4.dp,
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clickable(role = Role.Button) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        color = statusColor,
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.rating_anilist),
                        contentDescription = "AniList Tracker",
                        modifier = Modifier.size(26.dp),
                    )

                    // Active tracking indicator dot
                    if (isTrackingActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier
                .widthIn(min = 340.dp, max = 390.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(4.dp),
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
    val hapticFeedback = LocalHapticFeedback.current
    var tokenInput by remember { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }
    var manualSearchText by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
    ) {
        // --- 1. HEADER BAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = Color(0xFF00A2FF).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.rating_anilist),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = "AniList Tracker",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (trackerState.isAuthenticated) {
                    val user = trackerState.user
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val avatarUrl = user?.avatarUrl
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Text(
                                text = user?.name ?: "Connected",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. AUTHENTICATION PROMPT (If not logged in) ---
        if (!trackerState.isAuthenticated) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Connect your AniList account to sync watch progress, ratings, dates, notes, and custom lists across devices.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A2FF),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorize", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showTokenInput = true },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Paste Token", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                placeholder = { Text("Paste AniList Token / Pin URL...", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
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
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Save Token", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { showTokenInput = false },
                                ) {
                                    Text("Cancel", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            return@Column
        }

        // --- 3. LOADING STATE ---
        if (trackerState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF00A2FF),
                    )
                    Text(
                        text = "Matching anime on AniList...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Column
        }

        // --- 4. UNMATCHED ANIME STATE ---
        if (trackerState.media == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = trackerState.error ?: "Could not automatically match this anime on AniList.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Auto-Detect", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualSearchText,
                        onValueChange = { manualSearchText = it },
                        placeholder = { Text("Search title or enter AniList ID...", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
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
                        shape = RoundedCornerShape(10.dp),
                        enabled = manualSearchText.isNotBlank(),
                    ) {
                        Text("Search AniList", fontSize = 12.sp)
                    }
                }
            }
            return@Column
        }

        val media = trackerState.media!!
        val entry = trackerState.entry

        // --- 5. MATCHED ANIME IDENTITY CARD ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Cover Image
                val cover = media.coverImage?.large ?: media.coverImage?.medium
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 62.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (cover != null) {
                        AsyncImage(
                            model = cover,
                            contentDescription = media.title?.displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = media.title?.displayTitle ?: "Anime",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val metaLine = listOfNotNull(
                        media.format?.takeIf { it.isNotBlank() },
                        media.episodes?.let { "$it eps" },
                        media.startDateYear?.toString(),
                    ).joinToString(" • ")

                    if (metaLine.isNotBlank()) {
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }

                    if (media.averageScore != null && media.averageScore > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = "${media.averageScore}% community score",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 6. WATCH STATUS CAPSULES GRID ---
        Text(
            text = "Watch Status",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val currentStatus = entry?.status
        val statuses = listOf(
            Triple(AnilistMediaListStatus.CURRENT, Icons.Outlined.PlayCircle, StatusColorWatching),
            Triple(AnilistMediaListStatus.PLANNING, Icons.Default.CalendarToday, StatusColorPlanning),
            Triple(AnilistMediaListStatus.COMPLETED, Icons.Outlined.CheckCircle, StatusColorCompleted),
            Triple(AnilistMediaListStatus.PAUSED, Icons.Outlined.PauseCircle, StatusColorOnHold),
            Triple(AnilistMediaListStatus.DROPPED, Icons.Outlined.RemoveCircleOutline, StatusColorDropped),
            Triple(AnilistMediaListStatus.REPEATING, Icons.Default.Replay, StatusColorRepeating),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            statuses.chunked(2).forEach { rowStatuses ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowStatuses.forEach { (status, icon, color) ->
                        val isSelected = currentStatus == status
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    AnilistTrackerCoordinator.updateStatus(status)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) color.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = status.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                    ),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 7. EPISODE PROGRESS STEPPER & PROGRESS BAR ---
        val episodesTotal = media.episodes
        val currentProgress = entry?.progress ?: 0
        val activeColor = getStatusColor(currentStatus)
        val progressFraction = if (episodesTotal != null && episodesTotal > 0) {
            (currentProgress.toFloat() / episodesTotal).coerceIn(0f, 1f)
        } else {
            0f
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Episode Progress",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = if (episodesTotal != null) {
                            "Ep $currentProgress / $episodesTotal"
                        } else {
                            "Ep $currentProgress"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                        ),
                        color = activeColor,
                    )
                }

                if (episodesTotal != null && episodesTotal > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = tween(300),
                        label = "ProgressBar",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = activeColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Decrement Button
                    OutlinedButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            AnilistTrackerCoordinator.decrementProgress()
                        },
                        enabled = currentProgress > 0,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus 1", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Increment Button
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            AnilistTrackerCoordinator.incrementProgress()
                        },
                        enabled = episodesTotal == null || currentProgress < episodesTotal,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Plus 1", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 Ep", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 8. SCORE RATING SECTION (1-10 STARS) ---
        val currentScore = entry?.score ?: 0.0
        val scoreInt = currentScore.toInt()

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    val ratingLabel = when (scoreInt) {
                        10 -> "10 • Masterpiece"
                        9 -> "9 • Great"
                        8 -> "8 • Very Good"
                        7 -> "7 • Good"
                        6 -> "6 • Fine"
                        5 -> "5 • Average"
                        4 -> "4 • Bad"
                        3 -> "3 • Very Bad"
                        2 -> "2 • Horrible"
                        1 -> "1 • Appalling"
                        else -> "Unrated"
                    }

                    Text(
                        text = ratingLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = if (scoreInt > 0) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Star Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..10).forEach { starIndex ->
                        val isFilled = starIndex <= scoreInt
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newScore = if (scoreInt == starIndex) 0.0 else starIndex.toDouble()
                                    AnilistTrackerCoordinator.updateScore(newScore)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rate $starIndex",
                                modifier = Modifier.size(20.dp),
                                tint = if (isFilled) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 9. ADVANCED DETAILS (Dates, Repeat, Privacy, Notes) ---
        var showAdvancedOptions by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAdvancedOptions = !showAdvancedOptions },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Advanced Tracking Details",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = if (showAdvancedOptions) "Hide ▲" else "Expand ▼",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                AnimatedVisibility(
                    visible = showAdvancedOptions,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Start Date
                        val startedAt = entry?.startedAt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column {
                                    Text(
                                        text = "Start Date",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    Text(
                                        text = startedAt?.formatted() ?: "Not set",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (startedAt?.isSet == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        val now = io.ktor.util.date.GMTDate()
                                        val today = AnilistFuzzyDate(
                                            year = now.year,
                                            month = now.month.ordinal + 1,
                                            day = now.dayOfMonth,
                                        )
                                        AnilistTrackerCoordinator.updateStartedAt(today)
                                    },
                                ) {
                                    Text("Set Today", fontSize = 11.sp)
                                }
                                if (startedAt?.isSet == true) {
                                    IconButton(
                                        onClick = { AnilistTrackerCoordinator.updateStartedAt(null) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Finish / End Date
                        val completedAt = entry?.completedAt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column {
                                    Text(
                                        text = "Finish Date",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    Text(
                                        text = completedAt?.formatted() ?: "Not set",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (completedAt?.isSet == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        val now = io.ktor.util.date.GMTDate()
                                        val today = AnilistFuzzyDate(
                                            year = now.year,
                                            month = now.month.ordinal + 1,
                                            day = now.dayOfMonth,
                                        )
                                        AnilistTrackerCoordinator.updateCompletedAt(today)
                                    },
                                ) {
                                    Text("Set Today", fontSize = 11.sp)
                                }
                                if (completedAt?.isSet == true) {
                                    IconButton(
                                        onClick = { AnilistTrackerCoordinator.updateCompletedAt(null) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Repeat Count Stepper
                        val currentRepeat = entry?.repeat ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column {
                                    Text(
                                        text = "Rewatch Count",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    Text(
                                        text = "$currentRepeat times rewatched",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentRepeat > 0) {
                                                AnilistTrackerCoordinator.updateRepeat(currentRepeat - 1)
                                            }
                                        },
                                        enabled = currentRepeat > 0,
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Text(
                                    text = "$currentRepeat",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )

                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    IconButton(
                                        onClick = { AnilistTrackerCoordinator.updateRepeat(currentRepeat + 1) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Private Entry Switch
                        val isPrivate = entry?.private ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column {
                                    Text(
                                        text = "Private Entry",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    Text(
                                        text = "Hide this anime from your public profile",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Switch(
                                checked = isPrivate,
                                onCheckedChange = { AnilistTrackerCoordinator.updatePrivate(it) },
                                modifier = Modifier.size(width = 44.dp, height = 24.dp),
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Hidden from Status Lists Switch
                        val isHidden = entry?.hiddenFromStatusLists ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column {
                                    Text(
                                        text = "Hide from Status Lists",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    )
                                    Text(
                                        text = "Only show in custom lists",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Switch(
                                checked = isHidden,
                                onCheckedChange = { AnilistTrackerCoordinator.updateHiddenFromStatusLists(it) },
                                modifier = Modifier.size(width = 44.dp, height = 24.dp),
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Personal Notes Text Field
                        var notesText by remember(entry?.notes) { mutableStateOf(entry?.notes.orEmpty()) }
                        var isNotesDirty by remember(entry?.notes) { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Personal Notes",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }

                            OutlinedTextField(
                                value = notesText,
                                onValueChange = {
                                    notesText = it
                                    isNotesDirty = true
                                },
                                placeholder = { Text("Write your thoughts or reminders...", fontSize = 11.sp) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (isNotesDirty) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Button(
                                        onClick = {
                                            AnilistTrackerCoordinator.updateNotes(notesText)
                                            isNotesDirty = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("Save Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 10. REMOVE FROM ANILIST & MATCH DIAGNOSTICS ---
        if (entry != null) {
            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    AnilistTrackerCoordinator.deleteEntry()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remove from AniList",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        // Expandable Match Diagnostics
        if (!trackerState.debugInfo.isNullOrBlank()) {
            var showDiagnostics by remember { mutableStateOf(false) }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDiagnostics = !showDiagnostics },
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Match Diagnostics",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (showDiagnostics) "Hide ▲" else "View ▼",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            AnimatedVisibility(
                visible = showDiagnostics,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val clipboardManager = LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                ) {
                    Text(
                        text = trackerState.debugInfo.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(trackerState.debugInfo.orEmpty()))
                            copied = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Log",
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "✓ Log Copied to Clipboard!" else "Copy Diagnostics Log",
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}
