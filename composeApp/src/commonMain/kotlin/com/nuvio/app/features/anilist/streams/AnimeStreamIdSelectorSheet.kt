package com.nuvio.app.features.anilist.streams

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeStreamIdSelectorSheet(
    anilistId: Int,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
    isMovie: Boolean = false,
    onDismiss: () -> Unit,
    onOptionSelected: ((AnimeStreamIdOption) -> Unit)? = null,
) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsState()
    val options = remember(anilistId, prefs) { AnimeStreamIdManager.getOptions(anilistId) }
    val activeOption = remember(anilistId, prefs) { AnimeStreamIdManager.getActiveOption(anilistId) }
    var showCustomInput by remember { mutableStateOf(false) }
    var customIdInput by remember { mutableStateOf(if (activeOption.type == AnimeStreamIdType.CUSTOM) activeOption.rawId else "") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = appIconPainter(AppIconResource.PlayerPlay),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Stream Provider ID Switcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Select which ID to send to installed stream addons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    val isSelected = activeOption.type == option.type && (option.type != AnimeStreamIdType.CUSTOM || activeOption.rawId == option.rawId)
                    val queryPreview = AnimeStreamIdFormatter.formatVideoId(
                        option = option,
                        season = seasonNumber ?: 1,
                        episode = episodeNumber ?: 1,
                        isMovie = isMovie,
                    )
                    StreamIdOptionCard(
                        option = option,
                        queryPreview = queryPreview,
                        isSelected = isSelected,
                        onClick = {
                            AnimeStreamIdManager.selectOption(anilistId, option.type)
                            onOptionSelected?.invoke(option)
                            onDismiss()
                        },
                    )
                }

                // Custom ID Option
                val isCustomSelected = activeOption.type == AnimeStreamIdType.CUSTOM
                val customVal = if (isCustomSelected) activeOption.rawId else customIdInput
                val customQueryPreview = if (customVal.isNotBlank()) {
                    AnimeStreamIdFormatter.formatVideoId(
                        option = AnimeStreamIdOption(
                            type = AnimeStreamIdType.CUSTOM,
                            rawId = customVal,
                            formattedLabel = "Custom ($customVal)",
                            description = "",
                        ),
                        season = seasonNumber ?: 1,
                        episode = episodeNumber ?: 1,
                        isMovie = isMovie,
                    )
                } else null

                StreamIdCustomCard(
                    isSelected = isCustomSelected,
                    customValue = customVal,
                    queryPreview = customQueryPreview,
                    isEditing = showCustomInput,
                    onToggleEdit = { showCustomInput = !showCustomInput },
                    onSaveCustom = { entered ->
                        if (entered.isNotBlank()) {
                            AnimeStreamIdManager.selectOption(anilistId, AnimeStreamIdType.CUSTOM, entered.trim())
                            val customOpt = AnimeStreamIdOption(
                                type = AnimeStreamIdType.CUSTOM,
                                rawId = entered.trim(),
                                formattedLabel = "Custom (${entered.trim()})",
                                description = "User specified ID override",
                            )
                            onOptionSelected?.invoke(customOpt)
                            showCustomInput = false
                            onDismiss()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StreamIdOptionCard(
    option: AnimeStreamIdOption,
    queryPreview: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(14.dp)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        color = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${option.type.displayName} ($queryPreview)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    if (option.isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        ) {
                            Text(
                                text = "DEFAULT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
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
    onToggleEdit: () -> Unit,
    onSaveCustom: (String) -> Unit,
) {
    var text by remember(customValue) { mutableStateOf(customValue) }
    val cardShape = RoundedCornerShape(14.dp)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = cardShape),
        shape = cardShape,
        color = bgColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleEdit),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!queryPreview.isNullOrBlank()) "Custom ($queryPreview)" else "Custom Manual ID Override",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Manually specify any IMDb (tt...), Kitsu, or show ID to test scrapers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Custom ID",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(visible = isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. tt1234567, kitsu:1234, or raw ID") },
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = { onSaveCustom(text) },
                            enabled = text.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Apply & Use ID")
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
) {
    val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
    if (!anilistPrefs.enabled) return

    val options = remember(anilistId, anilistPrefs) {
        AnimeStreamIdManager.getOptions(anilistId)
    }
    val activeOption = remember(anilistId, anilistPrefs) {
        AnimeStreamIdManager.getActiveOption(anilistId)
    }
    var showSheet by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .nuvioDesktopDragScroll(scrollState)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = appIconPainter(AppIconResource.PlayerPlay),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "STREAM ID:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                )
            }
        }

        options.forEach { option ->
            val isSelected = activeOption.type == option.type &&
                (option.type != AnimeStreamIdType.CUSTOM || activeOption.rawId == option.rawId)
            val fullOptionId = AnimeStreamIdFormatter.formatVideoId(
                option = option,
                season = seasonNumber ?: 1,
                episode = episodeNumber ?: 1,
                isMovie = isMovie,
            )
            val chipLabel = when (option.type) {
                AnimeStreamIdType.CUSTOM -> "Custom ($fullOptionId)"
                else -> "${option.type.displayName} ($fullOptionId)"
            }
            val chipShape = RoundedCornerShape(10.dp)
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

            Surface(
                modifier = Modifier
                    .clip(chipShape)
                    .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = chipShape)
                    .clickable {
                        AnimeStreamIdManager.selectOption(anilistId, option.type)
                        onOptionChanged(fullOptionId)
                    },
                shape = chipShape,
                color = bgColor,
                contentColor = contentColor,
            ) {
                Text(
                    text = chipLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), shape = RoundedCornerShape(10.dp))
                .clickable { showSheet = true },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Custom",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Custom...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.width(32.dp))
    }

    if (showSheet) {
        AnimeStreamIdSelectorSheet(
            anilistId = anilistId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            isMovie = isMovie,
            onDismiss = { showSheet = false },
            onOptionSelected = { selectedOpt ->
                val newId = AnimeStreamIdFormatter.formatVideoId(
                    option = selectedOpt,
                    season = seasonNumber ?: 1,
                    episode = episodeNumber ?: 1,
                    isMovie = isMovie,
                )
                onOptionChanged(newId)
            },
        )
    }
}
