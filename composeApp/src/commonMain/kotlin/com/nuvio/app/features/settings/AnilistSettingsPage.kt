package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.features.anilist.AnilistSectionSettings
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.round
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistBrandBlue
import com.nuvio.app.features.anilist.AnilistLogoVector
import com.nuvio.app.features.anilist.AnilistPosterScoreFormat
import com.nuvio.app.features.anilist.AnilistPreferences
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.anilist.AnilistScoreFormat
import com.nuvio.app.features.anilist.AnilistTitleLanguage
import com.nuvio.app.features.anilist.AnilistUser
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_anilist
import nuvio.composeapp.generated.resources.settings_anilist_auto_complete_desc
import nuvio.composeapp.generated.resources.settings_anilist_auto_complete_title
import nuvio.composeapp.generated.resources.settings_anilist_auto_mark_desc
import nuvio.composeapp.generated.resources.settings_anilist_auto_mark_title
import nuvio.composeapp.generated.resources.settings_anilist_auto_move_watching_desc
import nuvio.composeapp.generated.resources.settings_anilist_auto_move_watching_title
import nuvio.composeapp.generated.resources.settings_anilist_notification_desc
import nuvio.composeapp.generated.resources.settings_anilist_notification_title
import nuvio.composeapp.generated.resources.settings_anilist_score_format_desc
import nuvio.composeapp.generated.resources.settings_anilist_score_format_title
import nuvio.composeapp.generated.resources.settings_anilist_section_account
import nuvio.composeapp.generated.resources.settings_anilist_section_display
import nuvio.composeapp.generated.resources.settings_anilist_section_playback
import nuvio.composeapp.generated.resources.settings_anilist_threshold_desc
import nuvio.composeapp.generated.resources.settings_anilist_threshold_title
import nuvio.composeapp.generated.resources.settings_anilist_title_lang_desc
import nuvio.composeapp.generated.resources.settings_anilist_title_lang_title
import org.jetbrains.compose.resources.stringResource

private enum class AnilistPickerType {
    TITLE_LANGUAGE,
    SCORE_FORMAT,
    POSTER_SCORE_FORMAT,
}

internal fun LazyListScope.anilistSettingsContent(
    isTablet: Boolean,
) {
    item {
        AnilistMasterSwitchSection(isTablet = isTablet)
    }

    item {
        val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
        if (prefs.enabled) {
            AnilistAccountSection(isTablet = isTablet)
        }
    }

    item {
        val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
        if (prefs.enabled) {
            AnilistPlaybackSection(isTablet = isTablet)
        }
    }

    item {
        val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
        if (prefs.enabled) {
            AnilistPosterDisplaySection(isTablet = isTablet)
        }
    }

    item {
        val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
        if (prefs.enabled) {
            AnilistLibrarySectionsSection(isTablet = isTablet)
        }
    }

    item {
        val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
        if (prefs.enabled) {
            AnilistDisplayPreferencesSection(isTablet = isTablet)
        }
    }
}

@Composable
private fun AnilistMasterSwitchSection(isTablet: Boolean) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()

    SettingsSection(
        title = "AniList Integration (Nuvio-Kai)",
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsSwitchRow(
                title = "Enable AniList Integration",
                description = "Master kill switch. When turned off, completely removes all AniList catalogs, tracking, metadata enhancements, and reverts Nuvio to standard stock behavior.",
                checked = prefs.enabled,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setEnabled,
            )
        }
    }
}

@Composable
private fun AnilistPosterDisplaySection(isTablet: Boolean) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    var activePicker by rememberSaveable { mutableStateOf<String?>(null) }

    SettingsSection(
        title = "Poster Display Options",
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsSwitchRow(
                title = "Show Title Logos on Posters",
                description = "Display transparent anime title logos centered over poster cards across shelves and catalogs.",
                checked = prefs.showPosterTitleLogos,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setShowPosterTitleLogos,
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = "Show AniList Scores",
                description = "Display native AniList rating badges in the top right corner of anime poster cards.",
                checked = prefs.showPosterAnilistScore,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setShowPosterAnilistScore,
            )

            if (prefs.showPosterAnilistScore) {
                SettingsGroupDivider(isTablet = isTablet)

                TrackingPreferenceActionRow(
                    title = "AniList Score Format",
                    description = "Choose how AniList ratings are formatted on poster cards (Percentage or 10-Point Score).",
                    value = prefs.posterScoreFormat.label,
                    isTablet = isTablet,
                    onClick = { activePicker = AnilistPickerType.POSTER_SCORE_FORMAT.name },
                )
            }

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = "Show MyAnimeList (MAL) Scores",
                description = "Display native MyAnimeList (MAL) rating badges in the top right corner of anime poster cards.",
                checked = prefs.showPosterMalScore,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setShowPosterMalScore,
            )
        }
    }

    when (activePicker) {
        AnilistPickerType.POSTER_SCORE_FORMAT.name -> {
            val scoreOptions = AnilistPosterScoreFormat.entries.map { fmt ->
                TrackingPickerOption(
                    value = fmt,
                    title = fmt.label,
                    description = when (fmt) {
                        AnilistPosterScoreFormat.PERCENTAGE -> "Display scores as percentage out of 100% (e.g., 84%)"
                        AnilistPosterScoreFormat.POINT_10 -> "Display scores on a 10-point scale (e.g., 8.4)"
                    },
                )
            }

            TrackingAdaptivePicker(
                isTablet = isTablet,
                title = "AniList Score Format",
                subtitle = "Select how you would like AniList ratings displayed on poster cards",
                selectedValue = prefs.posterScoreFormat,
                options = scoreOptions,
                onSelected = {
                    AnilistPreferencesRepository.setPosterScoreFormat(it)
                    activePicker = null
                },
                onDismiss = { activePicker = null },
            )
        }
    }
}

@Composable
private fun AnilistAccountSection(isTablet: Boolean) {
    val isAuth by AnilistAuthRepository.isAuthenticated.collectAsStateWithLifecycle()
    val user by AnilistAuthRepository.currentUser.collectAsStateWithLifecycle()
    val isAuthenticating by AnilistAuthRepository.isAuthenticating.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var showTokenDialog by rememberSaveable { mutableStateOf(false) }

    SettingsSection(
        title = stringResource(Res.string.settings_anilist_section_account),
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (isAuth && !user?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user?.avatarUrl,
                            contentDescription = user?.name,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = AnilistBrandBlue.copy(alpha = 0.15f),
                        ) {
                            Icon(
                                imageVector = AnilistLogoVector,
                                contentDescription = "AniList",
                                tint = AnilistBrandBlue,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(8.dp),
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isAuth) (user?.name ?: "Connected") else "AniList Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (isAuth) {
                                "Connected as ${user?.name ?: "User"} • Sync active"
                            } else {
                                "Connect your AniList account to track anime and sync lists"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isAuth) {
                    OutlinedButton(
                        onClick = { AnilistAuthRepository.logout() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = {
                            uriHandler.openUri(AnilistAuthRepository.OAUTH_AUTHORIZE_URL)
                            showTokenDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF02A9FF),
                            contentColor = Color.White,
                        ),
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        AnilistTokenDialog(onDismiss = { showTokenDialog = false })
    }
}

@Composable
private fun AnilistPlaybackSection(isTablet: Boolean) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    val isAuth by AnilistAuthRepository.isAuthenticated.collectAsStateWithLifecycle()

    SettingsSection(
        title = stringResource(Res.string.settings_anilist_section_playback),
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_anilist_auto_mark_title),
                description = stringResource(Res.string.settings_anilist_auto_mark_desc),
                checked = prefs.autoMarkEpisodeWatched,
                enabled = isAuth,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setAutoMarkEpisodeWatched,
            )

            SettingsGroupDivider(isTablet = isTablet)

            AnilistWatchedThresholdSliderRow(
                isTablet = isTablet,
                threshold = prefs.watchedPercentageThreshold,
                enabled = isAuth && prefs.autoMarkEpisodeWatched,
                onThresholdChange = AnilistPreferencesRepository::setWatchedPercentageThreshold,
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_anilist_auto_move_watching_title),
                description = stringResource(Res.string.settings_anilist_auto_move_watching_desc),
                checked = prefs.autoMoveToWatchingOnStart,
                enabled = isAuth && prefs.autoMarkEpisodeWatched,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setAutoMoveToWatchingOnStart,
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_anilist_auto_complete_title),
                description = stringResource(Res.string.settings_anilist_auto_complete_desc),
                checked = prefs.autoCompleteOnLastEpisode,
                enabled = isAuth && prefs.autoMarkEpisodeWatched,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setAutoCompleteOnLastEpisode,
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = "Automatically Add New Anime to AniList",
                description = "When enabled, watching an anime not in your AniList library will automatically add it to your Watching list and sync your progress.",
                checked = prefs.autoAddNewAnime,
                enabled = isAuth && prefs.autoMarkEpisodeWatched,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setAutoAddNewAnime,
            )

            SettingsGroupDivider(isTablet = isTablet)

            SettingsSwitchRow(
                title = stringResource(Res.string.settings_anilist_notification_title),
                description = stringResource(Res.string.settings_anilist_notification_desc),
                checked = prefs.showSyncNotification,
                enabled = isAuth && prefs.autoMarkEpisodeWatched,
                isTablet = isTablet,
                onCheckedChange = AnilistPreferencesRepository::setShowSyncNotification,
            )
        }
    }
}

@Composable
private fun AnilistLibrarySectionsSection(isTablet: Boolean) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    val isAuth by AnilistAuthRepository.isAuthenticated.collectAsStateWithLifecycle()

    val sections = remember(prefs.librarySections, isAuth) {
        AnilistPreferencesRepository.getEffectiveSections(isAuth)
    }

    SettingsSection(
        title = "Customize Catalogs & Sections",
        isTablet = isTablet,
        actions = {
            NuvioActionLabel(
                text = "Reset",
                onClick = {
                    AnilistPreferencesRepository.resetLibrarySections(isAuth)
                },
            )
        },
    ) {
        AnilistSectionsList(
            isTablet = isTablet,
            items = sections,
            isAuth = isAuth,
        )
    }
}

@Composable
private fun AnilistSectionsList(
    isTablet: Boolean,
    items: List<AnilistSectionSettings>,
    isAuth: Boolean,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        AnilistPreferencesRepository.moveSection(from.index, to.index, isAuth)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    SettingsGroup(isTablet = isTablet) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isTablet) 550.dp else 400.dp),
            state = lazyListState,
        ) {
            itemsIndexed(items, key = { _, item -> item.type }) { index, item ->
                ReorderableItem(
                    reorderableLazyListState,
                    key = item.type,
                ) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                    Surface(shadowElevation = elevation) {
                        Column {
                            if (index > 0) {
                                SettingsGroupDivider(isTablet = isTablet)
                            }
                            AnilistSectionSettingsRow(
                                item = item,
                                isTablet = isTablet,
                                onEnabledChange = { enabled ->
                                    AnilistPreferencesRepository.setSectionEnabled(item.type, enabled, isAuth)
                                },
                                dragHandleScope = this@ReorderableItem,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnilistSectionSettingsRow(
    item: AnilistSectionSettings,
    isTablet: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 14.dp
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.type,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textMuted,
                    uncheckedTrackColor = tokens.colors.borderDefault,
                ),
            )
            IconButton(
                modifier = with(dragHandleScope) {
                    Modifier.draggableHandle(
                        onDragStarted = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    )
                },
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Reorder",
                    tint = tokens.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AnilistWatchedThresholdSliderRow(
    isTablet: Boolean,
    threshold: Int,
    enabled: Boolean,
    onThresholdChange: (Int) -> Unit,
) {
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_anilist_threshold_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.settings_anilist_threshold_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "${sliderValue.roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue.coerceIn(50f, 95f),
            onValueChange = { if (enabled) sliderValue = (round(it / 5f) * 5f).coerceIn(50f, 95f) },
            onValueChangeFinished = {
                if (enabled) onThresholdChange(sliderValue.roundToInt().coerceIn(50, 95))
            },
            enabled = enabled,
            valueRange = 50f..95f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "50%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f),
            )
            Text(
                text = "75%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f),
            )
            Text(
                text = "95%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f),
            )
        }
    }
}

@Composable
private fun AnilistDisplayPreferencesSection(isTablet: Boolean) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    var activePicker by rememberSaveable { mutableStateOf<String?>(null) }

    SettingsSection(
        title = stringResource(Res.string.settings_anilist_section_display),
        isTablet = isTablet,
    ) {
        SettingsGroup(isTablet = isTablet) {
            TrackingPreferenceActionRow(
                title = stringResource(Res.string.settings_anilist_title_lang_title),
                description = stringResource(Res.string.settings_anilist_title_lang_desc),
                value = prefs.preferredTitleLanguage.label,
                isTablet = isTablet,
                onClick = { activePicker = AnilistPickerType.TITLE_LANGUAGE.name },
            )

            SettingsGroupDivider(isTablet = isTablet)

            TrackingPreferenceActionRow(
                title = stringResource(Res.string.settings_anilist_score_format_title),
                description = stringResource(Res.string.settings_anilist_score_format_desc),
                value = prefs.preferredScoreFormat.label,
                isTablet = isTablet,
                onClick = { activePicker = AnilistPickerType.SCORE_FORMAT.name },
            )
        }
    }

    when (activePicker) {
        AnilistPickerType.TITLE_LANGUAGE.name -> {
            val languageOptions = AnilistTitleLanguage.entries.map { lang ->
                TrackingPickerOption(
                    value = lang,
                    title = lang.label,
                    description = when (lang) {
                        AnilistTitleLanguage.ROMAJI -> "Display anime titles in romanized Japanese (e.g., Shingeki no Kyojin)"
                        AnilistTitleLanguage.ENGLISH -> "Display anime titles in English (e.g., Attack on Titan)"
                        AnilistTitleLanguage.NATIVE -> "Display anime titles in native Japanese Kanji / Kana"
                    },
                )
            }

            TrackingAdaptivePicker(
                isTablet = isTablet,
                title = stringResource(Res.string.settings_anilist_title_lang_title),
                subtitle = stringResource(Res.string.settings_anilist_title_lang_desc),
                selectedValue = prefs.preferredTitleLanguage,
                options = languageOptions,
                onSelected = {
                    AnilistPreferencesRepository.setPreferredTitleLanguage(it)
                    activePicker = null
                },
                onDismiss = { activePicker = null },
            )
        }

        AnilistPickerType.SCORE_FORMAT.name -> {
            val scoreOptions = AnilistScoreFormat.entries.map { format ->
                TrackingPickerOption(
                    value = format,
                    title = format.label,
                    description = when (format) {
                        AnilistScoreFormat.POINT_10_DECIMAL -> "Fine ratings with decimal precision (e.g., 8.5 / 10)"
                        AnilistScoreFormat.POINT_100 -> "Percentage score scale (e.g., 85 / 100)"
                        AnilistScoreFormat.POINT_5 -> "Classic 5-star scoring scale"
                        AnilistScoreFormat.POINT_3 -> "Simplified 3-tier rating (Liked, Neutral, Disliked)"
                    },
                )
            }

            TrackingAdaptivePicker(
                isTablet = isTablet,
                title = stringResource(Res.string.settings_anilist_score_format_title),
                subtitle = stringResource(Res.string.settings_anilist_score_format_desc),
                selectedValue = prefs.preferredScoreFormat,
                options = scoreOptions,
                onSelected = {
                    AnilistPreferencesRepository.setPreferredScoreFormat(it)
                    activePicker = null
                },
                onDismiss = { activePicker = null },
            )
        }
    }
}

@Composable
private fun AnilistTokenDialog(onDismiss: () -> Unit) {
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Connect AniList Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Authorize Nuvio in your browser, copy the access token, and paste it below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = {
                        tokenInput = it
                        errorMessage = null
                    },
                    label = { Text("Access Token") },
                    placeholder = { Text("Paste AniList token here") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tokenInput.isBlank()) {
                        errorMessage = "Please enter an access token"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val success = AnilistAuthRepository.loginWithToken(tokenInput)
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = "Invalid or expired token. Please try again."
                        }
                    }
                },
                enabled = !isSubmitting && tokenInput.isNotBlank(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Connect")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
            ) {
                Text("Cancel")
            }
        },
    )
}
