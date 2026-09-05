package com.nuvio.app.features.anilist.streams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.anilist.AnilistTrackerTheme
import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver
import com.nuvio.app.features.details.components.AnilistEmblemBgBrush
import com.nuvio.app.features.details.components.AnilistEmblemBorderBrush
import com.nuvio.app.features.details.components.LocalTrackerThemeTokens
import com.nuvio.app.features.details.components.ShapeAction
import com.nuvio.app.features.details.components.ShapeDialog
import com.nuvio.app.features.details.components.ShapeEmblem
import com.nuvio.app.features.details.components.ShapePill
import com.nuvio.app.features.details.components.ShapeRoundCapsule
import com.nuvio.app.features.details.components.ShapeSheetTop
import com.nuvio.app.features.details.components.ShapeTile
import com.nuvio.app.features.details.components.StatusColorWatching
import com.nuvio.app.features.details.components.TrackerGlassCard
import com.nuvio.app.features.details.components.TrackerThemeTokens
import com.nuvio.app.features.details.components.getTrackerThemeTokens
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeStreamIdSelectorSheet(
    anilistId: Int,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    relativeEpisodeNumber: Int? = null,
    isMovie: Boolean = false,
    onDismiss: () -> Unit,
    onOptionSelected: ((AnimeStreamIdOption) -> Unit)? = null,
) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsState()
    val currentTheme = prefs.trackerTheme
    val themeTokens = remember(currentTheme) { getTrackerThemeTokens(currentTheme) }

    val options = remember(anilistId, prefs) { AnimeStreamIdManager.getOptions(anilistId) }
    val activeOption = remember(anilistId, prefs) { AnimeStreamIdManager.getActiveOption(anilistId) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customIdInput by remember { mutableStateOf(if (activeOption.type == AnimeStreamIdType.CUSTOM) activeOption.rawId else "") }

    val offset = remember(anilistId) {
        AnilistMetaDetailsResolver.getCachedEpisodeOffset(anilistId)
    }
    val ep = episodeNumber ?: 1
    val effectiveRelativeEpisode = relativeEpisodeNumber ?: if (offset > 0 && ep > offset) (ep - offset) else ep

    CompositionLocalProvider(LocalTrackerThemeTokens provides themeTokens) {
        if (isDesktop) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 480.dp, max = 540.dp)
                        .clip(ShapeDialog),
                    shape = ShapeDialog,
                    color = themeTokens.dialogBackground,
                    border = BorderStroke(1.dp, themeTokens.dialogBorder),
                    shadowElevation = 24.dp,
                ) {
                    StreamIdSelectorContent(
                        anilistId = anilistId,
                        seasonNumber = seasonNumber,
                        episodeNumber = ep,
                        effectiveRelativeEpisode = effectiveRelativeEpisode,
                        isMovie = isMovie,
                        options = options,
                        activeOption = activeOption,
                        customIdInput = customIdInput,
                        showCustomInput = showCustomInput,
                        themeTokens = themeTokens,
                        onCustomInputChange = { customIdInput = it },
                        onToggleCustomInput = { showCustomInput = !showCustomInput },
                        onOptionSelected = { option ->
                            AnimeStreamIdManager.selectOption(anilistId, option.type)
                            onOptionSelected?.invoke(option)
                            onDismiss()
                        },
                        onSaveCustom = { customVal ->
                            if (customVal.isNotBlank()) {
                                AnimeStreamIdManager.selectOption(anilistId, AnimeStreamIdType.CUSTOM, customVal.trim())
                                val customOpt = AnimeStreamIdOption(
                                    type = AnimeStreamIdType.CUSTOM,
                                    rawId = customVal.trim(),
                                    formattedLabel = "Custom (${customVal.trim()})",
                                    description = "User specified ID override",
                                )
                                onOptionSelected?.invoke(customOpt)
                                showCustomInput = false
                                onDismiss()
                            }
                        },
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
                containerColor = themeTokens.dialogBackground,
                shape = ShapeSheetTop,
                showDragHandle = true,
            ) {
                StreamIdSelectorContent(
                    anilistId = anilistId,
                    seasonNumber = seasonNumber,
                    episodeNumber = ep,
                    effectiveRelativeEpisode = effectiveRelativeEpisode,
                    isMovie = isMovie,
                    options = options,
                    activeOption = activeOption,
                    customIdInput = customIdInput,
                    showCustomInput = showCustomInput,
                    themeTokens = themeTokens,
                    onCustomInputChange = { customIdInput = it },
                    onToggleCustomInput = { showCustomInput = !showCustomInput },
                    onOptionSelected = { option ->
                        AnimeStreamIdManager.selectOption(anilistId, option.type)
                        onOptionSelected?.invoke(option)
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                    onSaveCustom = { customVal ->
                        if (customVal.isNotBlank()) {
                            AnimeStreamIdManager.selectOption(anilistId, AnimeStreamIdType.CUSTOM, customVal.trim())
                            val customOpt = AnimeStreamIdOption(
                                type = AnimeStreamIdType.CUSTOM,
                                rawId = customVal.trim(),
                                formattedLabel = "Custom (${customVal.trim()})",
                                description = "User specified ID override",
                            )
                            onOptionSelected?.invoke(customOpt)
                            showCustomInput = false
                            coroutineScope.launch {
                                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                            }
                        }
                    },
                    onClose = {
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StreamIdSelectorContent(
    anilistId: Int,
    seasonNumber: Int?,
    episodeNumber: Int,
    effectiveRelativeEpisode: Int,
    isMovie: Boolean,
    options: List<AnimeStreamIdOption>,
    activeOption: AnimeStreamIdOption,
    customIdInput: String,
    showCustomInput: Boolean,
    themeTokens: TrackerThemeTokens,
    onCustomInputChange: (String) -> Unit,
    onToggleCustomInput: () -> Unit,
    onOptionSelected: (AnimeStreamIdOption) -> Unit,
    onSaveCustom: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // --- 1. PINNED APPLE GLASS HEADER BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeTokens.headerBackground)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(ShapeEmblem)
                            .background(AnilistEmblemBgBrush)
                            .border(1.dp, AnilistEmblemBorderBrush, ShapeEmblem),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = appIconPainter(AppIconResource.PlayerPlay),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Stream Provider ID Switcher",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = (-0.2).sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Select which ID to send to installed stream addons",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val hapticFeedback = LocalHapticFeedback.current
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(ShapePill)
                            .background(themeTokens.subtleChipBackground)
                            .border(1.dp, themeTokens.subtleChipBorder, ShapePill)
                            .clickable(role = Role.Button) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val nextTheme = when (themeTokens.theme) {
                                    AnilistTrackerTheme.FROSTED_GLASS -> AnilistTrackerTheme.WATER_GLASS
                                    AnilistTrackerTheme.WATER_GLASS -> AnilistTrackerTheme.MIDNIGHT_GLASS
                                    AnilistTrackerTheme.MIDNIGHT_GLASS -> AnilistTrackerTheme.SIDEBAR_GLASS
                                    AnilistTrackerTheme.SIDEBAR_GLASS -> AnilistTrackerTheme.FROSTED_GLASS
                                }
                                AnilistPreferencesRepository.setTrackerTheme(nextTheme)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Theme: ${themeTokens.theme.label}",
                            modifier = Modifier.size(15.dp),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(ShapePill)
                            .background(themeTokens.subtleChipBackground)
                            .border(1.dp, themeTokens.subtleChipBorder, ShapePill)
                            .clickable(role = Role.Button, onClick = onClose),
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

        // Hairline Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(themeTokens.headerHairline),
        )

        // --- 2. OPTIONS LIST ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { option ->
                val isSelected = activeOption.type == option.type &&
                    (option.type != AnimeStreamIdType.CUSTOM || activeOption.rawId == option.rawId)
                val effectiveSeason = if (option.season == 0 || seasonNumber == 0) 0 else (seasonNumber ?: option.season)
                val queryPreview = AnimeStreamIdFormatter.formatVideoId(
                    option = option,
                    season = effectiveSeason,
                    episode = episodeNumber,
                    isMovie = isMovie,
                    relativeEpisode = effectiveRelativeEpisode,
                )
                StreamIdOptionCard(
                    option = option,
                    queryPreview = queryPreview,
                    isSelected = isSelected,
                    themeTokens = themeTokens,
                    onClick = { onOptionSelected(option) },
                )
            }

            // Custom ID Option
            val isCustomSelected = activeOption.type == AnimeStreamIdType.CUSTOM
            val customVal = if (isCustomSelected) activeOption.rawId else customIdInput
            val customQueryPreview = if (customVal.isNotBlank()) {
                val effectiveCustomSeason = if (seasonNumber == 0) 0 else (seasonNumber ?: 1)
                AnimeStreamIdFormatter.formatVideoId(
                    option = AnimeStreamIdOption(
                        type = AnimeStreamIdType.CUSTOM,
                        rawId = customVal,
                        formattedLabel = "Custom ($customVal)",
                        description = "",
                        season = effectiveCustomSeason,
                    ),
                    season = effectiveCustomSeason,
                    episode = episodeNumber,
                    isMovie = isMovie,
                    relativeEpisode = effectiveRelativeEpisode,
                )
            } else null

            StreamIdCustomCard(
                isSelected = isCustomSelected,
                customValue = customVal,
                queryPreview = customQueryPreview,
                isEditing = showCustomInput,
                themeTokens = themeTokens,
                onToggleEdit = onToggleCustomInput,
                onSaveCustom = onSaveCustom,
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StreamIdOptionCard(
    option: AnimeStreamIdOption,
    queryPreview: String,
    isSelected: Boolean,
    themeTokens: TrackerThemeTokens,
    onClick: () -> Unit,
) {
    val selectedBg = themeTokens.statusSelectedBgs[StatusColorWatching]
    val selectedBorder = themeTokens.statusSelectedBorders[StatusColorWatching]

    TrackerGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTile,
        backgroundBrush = if (isSelected) selectedBg else themeTokens.cardBackground,
        backgroundColor = if (isSelected) null else themeTokens.cardBackgroundColor,
        borderBrush = if (isSelected) selectedBorder else themeTokens.cardBorder,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = option.type.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                        ),
                        color = if (isSelected) StatusColorWatching else Color.White,
                    )
                    if (option.isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusColorWatching.copy(alpha = 0.20f))
                                .border(1.dp, StatusColorWatching.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "DEFAULT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                ),
                                color = StatusColorWatching,
                            )
                        }
                    }
                    // Raw ID mono pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeTokens.subtleChipBackground)
                            .border(1.dp, themeTokens.subtleChipBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = queryPreview,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.55f),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Selection Indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(ShapePill)
                        .background(StatusColorWatching)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), ShapePill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(ShapePill)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), ShapePill),
                )
            }
        }
    }
}

@Composable
private fun StreamIdCustomCard(
    isSelected: Boolean,
    customValue: String,
    queryPreview: String?,
    isEditing: Boolean,
    themeTokens: TrackerThemeTokens,
    onToggleEdit: () -> Unit,
    onSaveCustom: (String) -> Unit,
) {
    var text by remember(customValue) { mutableStateOf(customValue) }
    val selectedBg = themeTokens.statusSelectedBgs[StatusColorWatching]
    val selectedBorder = themeTokens.statusSelectedBorders[StatusColorWatching]

    TrackerGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeTile,
        backgroundBrush = if (isSelected) selectedBg else themeTokens.cardBackground,
        backgroundColor = if (isSelected) null else themeTokens.cardBackgroundColor,
        borderBrush = if (isSelected) selectedBorder else themeTokens.cardBorder,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggleEdit),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Custom ID",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                            ),
                            color = if (isSelected) StatusColorWatching else Color.White,
                        )
                        if (!queryPreview.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeTokens.subtleChipBackground)
                                    .border(1.dp, themeTokens.subtleChipBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = queryPreview,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color.White.copy(alpha = 0.90f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Manually specify any IMDb (tt...), Kitsu, or show ID to test scrapers",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                        ),
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(ShapePill)
                        .background(themeTokens.subtleChipBackground)
                        .border(1.dp, themeTokens.subtleChipBorder, ShapePill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Custom ID",
                        tint = if (isSelected) StatusColorWatching else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            AnimatedVisibility(visible = isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "e.g. tt1234567, kitsu:1234, or raw ID",
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 13.sp,
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = themeTokens.subtleChipBackground,
                            unfocusedContainerColor = themeTokens.subtleChipBackground,
                            focusedBorderColor = StatusColorWatching,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = StatusColorWatching,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(ShapeAction)
                                .background(themeTokens.actionButtonBackground)
                                .border(1.dp, themeTokens.actionButtonBorder, ShapeAction)
                                .clickable(
                                    role = Role.Button,
                                    enabled = text.isNotBlank(),
                                    onClick = { onSaveCustom(text) },
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Apply & Use ID",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                ),
                                color = if (text.isNotBlank()) Color.White else Color.White.copy(alpha = 0.40f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimeStreamIdQuickBar(
    anilistId: Int,
    seasonNumber: Int?,
    episodeNumber: Int?,
    isMovie: Boolean,
    onOptionChanged: (newVideoId: String) -> Unit,
    modifier: Modifier = Modifier,
    relativeEpisodeNumber: Int? = null,
) {
    val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
    if (!anilistPrefs.enabled) return

    val currentTheme = anilistPrefs.trackerTheme
    val themeTokens = remember(currentTheme) { getTrackerThemeTokens(currentTheme) }

    val options = remember(anilistId, anilistPrefs) {
        AnimeStreamIdManager.getOptions(anilistId)
    }
    val activeOption = remember(anilistId, anilistPrefs) {
        AnimeStreamIdManager.getActiveOption(anilistId)
    }
    var showSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val offset = remember(anilistId) {
        AnilistMetaDetailsResolver.getCachedEpisodeOffset(anilistId)
    }
    val ep = episodeNumber ?: 1
    val effectiveRelativeEpisode = relativeEpisodeNumber ?: if (offset > 0 && ep > offset) (ep - offset) else ep

    Row(
        modifier = modifier
            .fillMaxWidth()
            .nuvioDesktopDragScroll(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // STREAM ID Capsule Label
        Box(
            modifier = Modifier
                .clip(ShapeRoundCapsule)
                .background(AnilistEmblemBgBrush)
                .border(1.dp, AnilistEmblemBorderBrush, ShapeRoundCapsule)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painter = appIconPainter(AppIconResource.PlayerPlay),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "STREAM ID",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.5.sp,
                    ),
                    color = Color.White,
                )
            }
        }

        options.forEach { option ->
            val isSelected = activeOption.type == option.type &&
                (option.type != AnimeStreamIdType.CUSTOM || activeOption.rawId == option.rawId)
            val effectiveSeason = if (option.season == 0 || seasonNumber == 0) 0 else (seasonNumber ?: option.season)
            val fullOptionId = AnimeStreamIdFormatter.formatVideoId(
                option = option,
                season = effectiveSeason,
                episode = ep,
                isMovie = isMovie,
                relativeEpisode = effectiveRelativeEpisode,
            )
            val chipLabel = when (option.type) {
                AnimeStreamIdType.CUSTOM -> "Custom ($fullOptionId)"
                else -> "${option.type.displayName} ($fullOptionId)"
            }

            val chipBg = if (isSelected) {
                themeTokens.statusSelectedBgs[StatusColorWatching]
            } else {
                null
            }
            val chipBorder = if (isSelected) {
                themeTokens.statusSelectedBorders[StatusColorWatching]
            } else {
                themeTokens.subtleChipBorder
            }

            Box(
                modifier = Modifier
                    .clip(ShapeRoundCapsule)
                    .then(
                        if (chipBg != null) Modifier.background(chipBg)
                        else Modifier.background(themeTokens.subtleChipBackground)
                    )
                    .then(
                        if (chipBorder != null) Modifier.border(1.dp, chipBorder, ShapeRoundCapsule)
                        else Modifier
                    )
                    .clickable(role = Role.Button) {
                        AnimeStreamIdManager.selectOption(anilistId, option.type)
                        onOptionChanged(fullOptionId)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(ShapePill)
                                .background(Color.White),
                        )
                    }
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                        ),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // Custom... button
        Box(
            modifier = Modifier
                .clip(ShapeRoundCapsule)
                .background(themeTokens.subtleChipBackground)
                .border(1.dp, themeTokens.subtleChipBorder, ShapeRoundCapsule)
                .clickable(role = Role.Button) { showSheet = true }
                .padding(horizontal = 11.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Custom",
                    modifier = Modifier.size(13.dp),
                    tint = Color.White.copy(alpha = 0.75f),
                )
                Text(
                    text = "Custom...",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))
    }

    if (showSheet) {
        AnimeStreamIdSelectorSheet(
            anilistId = anilistId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            relativeEpisodeNumber = effectiveRelativeEpisode,
            isMovie = isMovie,
            onDismiss = { showSheet = false },
            onOptionSelected = { selectedOpt ->
                val effectiveSeason = if (selectedOpt.season == 0 || seasonNumber == 0) 0 else (seasonNumber ?: selectedOpt.season)
                val newId = AnimeStreamIdFormatter.formatVideoId(
                    option = selectedOpt,
                    season = effectiveSeason,
                    episode = ep,
                    isMovie = isMovie,
                    relativeEpisode = effectiveRelativeEpisode,
                )
                onOptionChanged(newId)
            },
        )
    }
}
