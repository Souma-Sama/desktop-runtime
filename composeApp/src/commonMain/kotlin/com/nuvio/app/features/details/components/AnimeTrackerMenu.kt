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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistFuzzyDate
import com.nuvio.app.features.anilist.AnilistMediaListStatus
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.rating_anilist
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

// Status Color Tokens
private val StatusColorWatching = Color(0xFF00A2FF)
private val StatusColorPlanning = Color(0xFFA855F7)
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
private fun TrackerGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    backgroundColor: Color = Color.Transparent,
    backgroundBrush: Brush? = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.035f),
        ),
    ),
    borderBrush: Brush? = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.02f),
        ),
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bgModifier = if (backgroundBrush != null) {
        Modifier.background(backgroundBrush)
    } else {
        Modifier.background(backgroundColor)
    }

    val borderModifier = if (borderBrush != null) {
        Modifier.border(1.dp, borderBrush, shape)
    } else {
        Modifier
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(bgModifier)
            .then(borderModifier)
            .then(clickModifier),
    ) {
        // Specular top light refraction line (Water Glass gleam)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content,
        )
    }
}

@Composable
fun AnimeTrackerButton(
    modifier: Modifier = Modifier,
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    title: String? = null,
    size: Dp = 52.dp,
) {
    val anilistPrefs by com.nuvio.app.features.anilist.AnilistPreferencesRepository.preferences.collectAsState()
    val isKaiItem = remember(meta?.id) {
        AnilistTrackerCoordinator.isKaiMedia(meta?.id)
    }
    if (!anilistPrefs.enabled || !isKaiItem) return

    var showSheet by remember { mutableStateOf(false) }
    val trackerState by AnilistTrackerCoordinator.trackerState.collectAsState()
    val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: meta?.name.orEmpty()
    val hapticFeedback = LocalHapticFeedback.current

    val anilistId = remember(meta?.id) { AnilistTrackerCoordinator.extractAnilistId(meta?.id) }
    val currentStatus = trackerState.entry?.takeIf {
        trackerState.media != null && (
            (anilistId != null && trackerState.media?.id == anilistId) ||
            trackerState.lastLookupTitle.equals(effectiveTitle, ignoreCase = true)
        )
    }?.status ?: com.nuvio.app.features.anilist.AnilistLibraryRepository.getMediaStatusById(meta?.id.orEmpty(), effectiveTitle)

    val isTrackingActive = currentStatus != null
    val statusColor = getStatusColor(currentStatus)

    val containerColor by animateColorAsState(
        targetValue = when {
            isTrackingActive -> statusColor.copy(alpha = 0.22f)
            showSheet -> MaterialTheme.colorScheme.onBackground
            else -> Color(0xFF101524).copy(alpha = 0.82f)
        },
        animationSpec = tween(250),
        label = "TrackerButtonBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isTrackingActive -> statusColor.copy(alpha = 0.85f)
            showSheet -> MaterialTheme.colorScheme.primary
            else -> Color.White.copy(alpha = 0.22f)
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
                    width = if (isTrackingActive || showSheet) 1.5.dp else 1.dp,
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
                        showSheet = true
                        val metaYear = meta?.releaseInfo?.take(4)?.toIntOrNull()
                        AnilistTrackerCoordinator.loadForMedia(
                            title = effectiveTitle,
                            mediaId = meta?.id,
                            year = metaYear,
                            genres = meta?.genres.orEmpty(),
                            country = meta?.country,
                            language = meta?.language,
                            forceRefresh = false,
                        )
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

        if (showSheet) {
            AnimeTrackerSheet(
                meta = meta,
                title = effectiveTitle,
                onDismiss = { showSheet = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTrackerSheet(
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    preview: com.nuvio.app.features.home.MetaPreview? = null,
    title: String? = null,
    onDismiss: () -> Unit,
) {
    val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: preview?.name ?: meta?.name.orEmpty()
    val mediaId = preview?.id ?: meta?.id
    val year = preview?.releaseInfo?.take(4)?.toIntOrNull() ?: meta?.releaseInfo?.take(4)?.toIntOrNull()
    val genres = preview?.genres ?: meta?.genres.orEmpty()
    val country = meta?.country
    val language = meta?.language

    LaunchedEffect(effectiveTitle, mediaId) {
        if (effectiveTitle.isNotBlank()) {
            AnilistTrackerCoordinator.loadForMedia(
                title = effectiveTitle,
                mediaId = mediaId,
                year = year,
                genres = genres,
                country = country,
                language = language,
                forceRefresh = false,
            )
        }
    }

    if (isDesktop) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight(0.88f)
                    .widthIn(min = 480.dp, max = 530.dp)
                    .clip(RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0C101D).copy(alpha = 0.72f),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.15f),
                        ),
                    ),
                ),
                shadowElevation = 24.dp,
            ) {
                AnimeTrackerSheetContent(
                    meta = meta,
                    preview = preview,
                    title = effectiveTitle,
                    onClose = onDismiss,
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()

        NuvioModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                }
            },
            sheetState = sheetState,
            containerColor = Color(0xFF0C101D).copy(alpha = 0.88f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            showDragHandle = true,
        ) {
            AnimeTrackerSheetContent(
                meta = meta,
                preview = preview,
                title = effectiveTitle,
                onClose = {
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTrackerSheetContent(
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    preview: com.nuvio.app.features.home.MetaPreview? = null,
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
    var showUserProfileSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        // --- 1. PINNED APPLE WATER GLASS HEADER BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                        ),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00B4D8), Color(0xFF0077B6)),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.50f),
                                        Color.White.copy(alpha = 0.20f),
                                    ),
                                ),
                                shape = RoundedCornerShape(11.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.rating_anilist),
                            contentDescription = "AniList",
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "AniList Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = (-0.2).sp,
                            ),
                            color = Color.White,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (trackerState.isAuthenticated) Color(0xFF10B981) else Color(0xFFF59E0B)),
                            )
                            Text(
                                text = if (trackerState.isAuthenticated) "Cloud Synced" else "Not Logged In",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = Color.White.copy(alpha = 0.55f),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (trackerState.isAuthenticated) {
                        val user = trackerState.user
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.08f),
                                        ),
                                    ),
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable(role = Role.Button) { showUserProfileSheet = true }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val avatarUrl = user?.avatarUrl
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                Text(
                                    text = user?.name ?: "Connected",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.90f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.09f))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f),
                                    ),
                                ),
                                CircleShape,
                            )
                            .clickable(role = Role.Button) { onClose() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(15.dp),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // Frosted specular hairline divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // --- 2. SCROLLABLE WATER GLASS BODY ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            // --- AUTHENTICATION PROMPT (If not logged in) ---
            if (!trackerState.isAuthenticated) {
                TrackerGlassCard {
                    Text(
                        text = "Connect your AniList account to synchronize your watch history, scores, dates, notes, and custom lists across all your devices.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        color = Color.White.copy(alpha = 0.70f),
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!showTokenInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    uriHandler.openUri(AnilistAuthRepository.OAUTH_AUTHORIZE_URL)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A2FF),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorize", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showTokenInput = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            ) {
                                Text("Paste Token", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                placeholder = { Text("Paste AniList Token / Pin URL...", fontSize = 12.sp, color = Color.White.copy(0.35f)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00A2FF),
                                    unfocusedBorderColor = Color.White.copy(0.18f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00A2FF),
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text("Save Token", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { showTokenInput = false },
                                ) {
                                    Text("Cancel", fontSize = 13.sp, color = Color.White.copy(alpha = 0.60f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                return@Column
            }

            // --- LOADING STATE ---
            if (trackerState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF00A2FF),
                        )
                        Text(
                            text = "Matching anime on AniList...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                return@Column
            }

            // --- UNMATCHED ANIME STATE ---
            if (trackerState.media == null) {
                TrackerGlassCard {
                    Text(
                        text = trackerState.error ?: "Could not automatically match this anime on AniList.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val effectiveRetryTitle = title?.takeIf { it.isNotBlank() } ?: preview?.name ?: meta?.name.orEmpty()
                    if (effectiveRetryTitle.isNotBlank() || preview != null || meta != null) {
                        Button(
                            onClick = {
                                val metaYear = preview?.releaseInfo?.take(4)?.toIntOrNull() ?: meta?.releaseInfo?.take(4)?.toIntOrNull()
                                AnilistTrackerCoordinator.loadForMedia(
                                    title = effectiveRetryTitle,
                                    mediaId = preview?.id ?: meta?.id,
                                    year = metaYear,
                                    genres = preview?.genres ?: meta?.genres.orEmpty(),
                                    country = meta?.country,
                                    language = meta?.language,
                                    forceRefresh = true,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A2FF),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Auto-Detect", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualSearchText,
                        onValueChange = { manualSearchText = it },
                        placeholder = { Text("Search title or enter AniList ID...", fontSize = 12.sp, color = Color.White.copy(0.35f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A2FF),
                            unfocusedBorderColor = Color.White.copy(0.18f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                        shape = RoundedCornerShape(12.dp),
                        enabled = manualSearchText.isNotBlank(),
                    ) {
                        Text("Search AniList", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                return@Column
            }

            val media = trackerState.media!!
            val entry = trackerState.entry
            val isReading = remember(media.format) {
                com.nuvio.app.features.anilist.KaiHooks.isNonVideoMedia(media.format)
            }

            // --- 3. MATCHED ANIME / MANGA HERO CARD ---
            TrackerGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Cover Poster
                    val cover = media.coverImage?.large ?: media.coverImage?.medium
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.10f),
                                    ),
                                ),
                                RoundedCornerShape(12.dp),
                            ),
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

                    // Metadata Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = media.title?.displayTitle ?: if (isReading) "Manga" else "Anime",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val metaLine = if (isReading) {
                            listOfNotNull(
                                media.format?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Manga",
                                media.chapters?.let { "$it ch" },
                                media.volumes?.let { "$it vol" },
                                media.startDateYear?.toString(),
                            ).joinToString(" • ")
                        } else {
                            listOfNotNull(
                                media.format?.takeIf { it.isNotBlank() },
                                media.episodes?.let { "$it eps" },
                                media.startDateYear?.toString(),
                            ).joinToString(" • ")
                        }

                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.55f),
                                maxLines = 1,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (media.averageScore != null && media.averageScore > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFFFFB800).copy(alpha = 0.14f))
                                        .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
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
                                            text = "${media.averageScore}% Score",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = Color(0xFFFFB800),
                                        )
                                    }
                                }
                            }

                            val mediaWebUrl = "https://anilist.co/${if (isReading) "manga" else "anime"}/${media.id}"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.08f),
                                            ),
                                        ),
                                        RoundedCornerShape(999.dp),
                                    )
                                    .clickable(role = Role.Button) { uriHandler.openUri(mediaWebUrl) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = "AniList",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = Color.White.copy(alpha = 0.80f),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.80f),
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4. STATUS CAPSULES (2x3 WATER GLASS TILES) ---
            Text(
                text = if (isReading) "Reading Status" else "Watch Status",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp,
                ),
                color = Color.White.copy(alpha = 0.90f),
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                statuses.chunked(2).forEach { rowStatuses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowStatuses.forEach { (status, icon, color) ->
                            val isSelected = currentStatus == status
                            val backgroundModifier = if (isSelected) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(
                                            color.copy(alpha = 0.36f),
                                            color.copy(alpha = 0.16f),
                                        ),
                                    ),
                                )
                            } else {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.065f),
                                            Color.White.copy(alpha = 0.025f),
                                        ),
                                    ),
                                )
                            }
                            val borderModifier = if (isSelected) {
                                Modifier.border(
                                    1.5.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            color.copy(alpha = 0.95f),
                                            color.copy(alpha = 0.60f),
                                        ),
                                    ),
                                    RoundedCornerShape(14.dp),
                                )
                            } else {
                                Modifier.border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.18f),
                                            Color.White.copy(alpha = 0.06f),
                                        ),
                                    ),
                                    RoundedCornerShape(14.dp),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .then(backgroundModifier)
                                    .then(borderModifier)
                                    .clickable(role = Role.Button) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AnilistTrackerCoordinator.updateStatus(status)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) color else Color.White.copy(alpha = 0.65f),
                                        )
                                        val statusLabel = when (status) {
                                            AnilistMediaListStatus.CURRENT -> if (isReading) "Reading" else "Watching"
                                            AnilistMediaListStatus.PLANNING -> if (isReading) "Plan to Read" else "Plan to Watch"
                                            AnilistMediaListStatus.REPEATING -> if (isReading) "Rereading" else "Rewatching"
                                            else -> status.label
                                        }
                                        Text(
                                            text = statusLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.5.sp,
                                            ),
                                            color = if (isSelected) color else Color.White.copy(alpha = 0.82f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 5. PROGRESS STEPPERS (CONTINUOUS WATER GLASS PILL) ---
            val activeColor = getStatusColor(currentStatus)
            if (isReading) {
                // Chapters Stepper
                TrackerProgressStepperSection(
                    title = "Chapter Progress",
                    icon = Icons.Default.EditNote,
                    unitLabel = "Ch",
                    currentUnits = entry?.progress ?: 0,
                    totalUnits = media.chapters,
                    accentColor = activeColor,
                    onIncrement = { AnilistTrackerCoordinator.incrementProgress() },
                    onDecrement = { AnilistTrackerCoordinator.decrementProgress() },
                    onSetExact = { target -> AnilistTrackerCoordinator.updateProgress(target) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Volumes Stepper
                val currentVolumes = entry?.progressVolumes ?: 0
                val totalVolumes = media.volumes
                TrackerProgressStepperSection(
                    title = "Volume Progress",
                    icon = Icons.Default.ContentCopy,
                    unitLabel = "Vol",
                    currentUnits = currentVolumes,
                    totalUnits = totalVolumes,
                    accentColor = Color(0xFFA855F7),
                    onIncrement = { AnilistTrackerCoordinator.updateProgressVolumes(currentVolumes + 1) },
                    onDecrement = { if (currentVolumes > 0) AnilistTrackerCoordinator.updateProgressVolumes(currentVolumes - 1) },
                    onSetExact = { target -> AnilistTrackerCoordinator.updateProgressVolumes(target) },
                )
            } else {
                // Episode Stepper (Anime)
                TrackerProgressStepperSection(
                    title = "Episode Progress",
                    icon = Icons.Default.EditNote,
                    unitLabel = "Ep",
                    currentUnits = entry?.progress ?: 0,
                    totalUnits = media.episodes,
                    accentColor = activeColor,
                    onIncrement = { AnilistTrackerCoordinator.incrementProgress() },
                    onDecrement = { AnilistTrackerCoordinator.decrementProgress() },
                    onSetExact = { target -> AnilistTrackerCoordinator.updateProgress(target) },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 6. SCORE RATING SECTION (1-10 STARS APPLE DIAL) ---
            val rawScore = entry?.score ?: 0.0
            val currentScore = if (rawScore >= 10.0) rawScore / 10.0 else rawScore
            val scoreInt = ((currentScore * 10.0).roundToInt() / 10.0).roundToInt()

            TrackerGlassCard {
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
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "Score Rating",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    if (scoreInt > 0) {
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
                            else -> "$scoreInt • Rated"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = ratingLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                                color = Color(0xFFFFB800),
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable(role = Role.Button) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AnilistTrackerCoordinator.updateScore(0.0)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Score",
                                    tint = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Unrated",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 10-Star Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..10).forEach { starIndex ->
                        val isFilled = starIndex <= scoreInt
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newScore = if (scoreInt == starIndex) 0.0 else starIndex.toDouble()
                                    AnilistTrackerCoordinator.updateScore(newScore)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rate $starIndex",
                                modifier = Modifier.size(22.dp),
                                tint = if (isFilled) Color(0xFFFFB800) else Color.White.copy(alpha = 0.22f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 7. TRACKING DATES (APPLE GROUPED INSET CARD) ---
            TrackerGlassCard {
                // Start Date
                val startedAt = entry?.startedAt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color.White.copy(alpha = 0.65f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = startedAt?.formatted() ?: "Not set",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = if (startedAt?.isSet == true) Color(0xFF00A2FF) else Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.08f),
                                        ),
                                    ),
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable(role = Role.Button) {
                                    val now = io.ktor.util.date.GMTDate()
                                    val today = AnilistFuzzyDate(
                                        year = now.year,
                                        month = now.month.ordinal + 1,
                                        day = now.dayOfMonth,
                                    )
                                    AnilistTrackerCoordinator.updateStartedAt(today)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Set Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                        if (startedAt?.isSet == true) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable(role = Role.Button) { AnilistTrackerCoordinator.updateStartedAt(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Finish / End Date
                val completedAt = entry?.completedAt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color.White.copy(alpha = 0.65f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Finish Date",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = completedAt?.formatted() ?: "Not set",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = if (completedAt?.isSet == true) Color(0xFF10B981) else Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0.08f),
                                        ),
                                    ),
                                    RoundedCornerShape(999.dp),
                                )
                                .clickable(role = Role.Button) {
                                    val now = io.ktor.util.date.GMTDate()
                                    val today = AnilistFuzzyDate(
                                        year = now.year,
                                        month = now.month.ordinal + 1,
                                        day = now.dayOfMonth,
                                    )
                                    AnilistTrackerCoordinator.updateCompletedAt(today)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Set Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                        if (completedAt?.isSet == true) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable(role = Role.Button) { AnilistTrackerCoordinator.updateCompletedAt(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 8. ADVANCED DETAILS (APPLE ACCORDION) ---
            var showAdvancedOptions by remember { mutableStateOf(false) }

            TrackerGlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { showAdvancedOptions = !showAdvancedOptions },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color(0xFF00A2FF),
                        )
                        Text(
                            text = "Advanced Tracking Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.08f),
                                    ),
                                ),
                                RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = if (showAdvancedOptions) "Hide ▲" else "Expand ▼",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            color = Color(0xFF00A2FF),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showAdvancedOptions,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Repeat Stepper
                        val currentRepeat = entry?.repeat ?: 0
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
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Column {
                                    Text(
                                        text = if (isReading) "Reread Count" else "Rewatch Count",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = if (isReading) "$currentRepeat times reread" else "$currentRepeat times rewatched",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.45f),
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable(
                                            enabled = currentRepeat > 0,
                                            role = Role.Button,
                                        ) {
                                            if (currentRepeat > 0) {
                                                AnilistTrackerCoordinator.updateRepeat(currentRepeat - 1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (currentRepeat > 0) Color.White else Color.White.copy(alpha = 0.20f),
                                    )
                                }

                                Text(
                                    text = "$currentRepeat",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable(role = Role.Button) {
                                            AnilistTrackerCoordinator.updateRepeat(currentRepeat + 1)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )

                        // Private Entry Switch
                        val isPrivate = entry?.private ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Column {
                                    Text(
                                        text = "Private Entry",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Hide this entry from your public profile",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.45f),
                                    )
                                }
                            }

                            Switch(
                                checked = isPrivate,
                                onCheckedChange = { AnilistTrackerCoordinator.updatePrivate(it) },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )

                        // Hidden from Status Lists Switch
                        val isHidden = entry?.hiddenFromStatusLists ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Column {
                                    Text(
                                        text = "Hide from Status Lists",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Only show in custom lists",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.45f),
                                    )
                                }
                            }

                            Switch(
                                checked = isHidden,
                                onCheckedChange = { AnilistTrackerCoordinator.updateHiddenFromStatusLists(it) },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )

                        // Personal Notes Text Field
                        var notesText by remember(entry?.notes) { mutableStateOf(entry?.notes.orEmpty()) }
                        var isNotesDirty by remember(entry?.notes) { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Text(
                                    text = "Personal Notes",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                    ),
                                    color = Color.White,
                                )
                            }

                            OutlinedTextField(
                                value = notesText,
                                onValueChange = {
                                    notesText = it
                                    isNotesDirty = true
                                },
                                placeholder = { Text("Write personal thoughts, reminders, or tags...", fontSize = 12.sp, color = Color.White.copy(0.35f)) },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00A2FF),
                                    unfocusedBorderColor = Color.White.copy(0.18f),
                                ),
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
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00A2FF),
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Text("Save Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 9. DESTRUCTIVE ACTION (GUARDED 2-STEP CONFIRMATION) ---
            if (entry != null) {
                Spacer(modifier = Modifier.height(14.dp))

                var confirmDelete by remember { mutableStateOf(false) }
                LaunchedEffect(confirmDelete) {
                    if (confirmDelete) {
                        delay(4000)
                        confirmDelete = false
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (confirmDelete) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.28f),
                                        Color(0xFFEF4444).copy(alpha = 0.16f),
                                    ),
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.14f),
                                        Color(0xFFEF4444).copy(alpha = 0.05f),
                                    ),
                                )
                            },
                        )
                        .border(
                            1.dp,
                            if (confirmDelete) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.85f),
                                        Color(0xFFEF4444).copy(alpha = 0.50f),
                                    ),
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.40f),
                                        Color(0xFFEF4444).copy(alpha = 0.15f),
                                    ),
                                )
                            },
                            RoundedCornerShape(14.dp),
                        )
                        .clickable(role = Role.Button) {
                            if (!confirmDelete) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                confirmDelete = true
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                AnilistTrackerCoordinator.deleteEntry()
                                onClose()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFEF4444),
                        )
                        Text(
                            text = if (confirmDelete) "Tap Again to Confirm Removal" else "Remove from AniList",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = Color(0xFFEF4444),
                        )
                    }
                }
            }

            // Expandable Match Diagnostics
            if (!trackerState.debugInfo.isNullOrBlank()) {
                var showDiagnostics by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { showDiagnostics = !showDiagnostics },
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
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.40f),
                        )
                        Text(
                            text = if (showDiagnostics) "Hide ▲" else "View ▼",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF00A2FF),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = trackerState.debugInfo.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(trackerState.debugInfo.orEmpty()))
                                copied = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Log",
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copied) "✓ Log Copied to Clipboard!" else "Copy Diagnostics Log",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.80f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showUserProfileSheet && trackerState.user != null) {
        com.nuvio.app.features.anilist.profile.AnilistUserProfileSheet(
            userId = trackerState.user?.id,
            username = trackerState.user?.name,
            onDismiss = { showUserProfileSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackerProgressStepperSection(
    title: String,
    icon: ImageVector,
    unitLabel: String,
    currentUnits: Int,
    totalUnits: Int?,
    accentColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetExact: (Int) -> Unit,
) {
    var showExactEditDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val fraction = if (totalUnits != null && totalUnits > 0) {
        (currentUnits.toFloat() / totalUnits).coerceIn(0f, 1f)
    } else 0f

    TrackerGlassCard {
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
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = accentColor,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                    color = Color.White,
                )
            }

            Text(
                text = if (totalUnits != null) {
                    "$unitLabel $currentUnits of $totalUnits"
                } else {
                    "$unitLabel $currentUnits"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                ),
                color = accentColor,
            )
        }

        if (totalUnits != null && totalUnits > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            val animatedProgress by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(300),
                label = "ProgressPillTrack",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, Color(0xFF38BDF8)),
                            ),
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Continuous Segmented Water Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.04f),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.08f),
                            ),
                        ),
                        RoundedCornerShape(13.dp),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Minus segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = currentUnits > 0,
                            role = Role.Button,
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDecrement()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp),
                        tint = if (currentUnits > 0) Color.White else Color.White.copy(alpha = 0.25f),
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.14f)),
                )

                // Middle: Direct input / badge segment
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                        .clickable(role = Role.Button) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showExactEditDialog = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "$unitLabel $currentUnits",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = accentColor,
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exact",
                            modifier = Modifier.size(12.dp),
                            tint = accentColor.copy(alpha = 0.70f),
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.14f)),
                )

                // Plus segment
                val canIncrement = totalUnits == null || currentUnits < totalUnits
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = canIncrement,
                            role = Role.Button,
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIncrement()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp),
                        tint = if (canIncrement) Color.White else Color.White.copy(alpha = 0.25f),
                    )
                }
            }

            // Quick Max completion button if total is known and not reached
            if (totalUnits != null && currentUnits < totalUnits) {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.28f),
                                    accentColor.copy(alpha = 0.14f),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.70f),
                                    accentColor.copy(alpha = 0.40f),
                                ),
                            ),
                            RoundedCornerShape(13.dp),
                        )
                        .clickable(role = Role.Button) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSetExact(totalUnits)
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor,
                        )
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                            color = accentColor,
                        )
                    }
                }
            }
        }
    }

    if (showExactEditDialog) {
        var textInput by remember { mutableStateOf(currentUnits.toString()) }
        BasicAlertDialog(
            onDismissRequest = { showExactEditDialog = false },
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0F1322).copy(alpha = 0.92f),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f),
                        ),
                    ),
                ),
                shadowElevation = 20.dp,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Set $unitLabel Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = Color.White,
                    )

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0", color = Color.White.copy(0.4f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(0.18f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showExactEditDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.80f), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val target = textInput.toIntOrNull() ?: currentUnits
                                val clamped = if (totalUnits != null) target.coerceIn(0, totalUnits) else target.coerceAtLeast(0)
                                onSetExact(clamped)
                                showExactEditDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
