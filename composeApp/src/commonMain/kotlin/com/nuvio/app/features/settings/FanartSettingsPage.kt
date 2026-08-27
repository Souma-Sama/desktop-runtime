package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.fanart.FanartArtworkQuality
import com.nuvio.app.features.fanart.FanartSettings
import com.nuvio.app.features.fanart.FanartSettingsRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.settings_fanart_add_api_key_first
import nuvio.composeapp.generated.resources.settings_fanart_api_key_description
import nuvio.composeapp.generated.resources.settings_fanart_api_key_title
import nuvio.composeapp.generated.resources.settings_fanart_backdrops
import nuvio.composeapp.generated.resources.settings_fanart_backdrops_description
import nuvio.composeapp.generated.resources.settings_fanart_banners
import nuvio.composeapp.generated.resources.settings_fanart_banners_description
import nuvio.composeapp.generated.resources.settings_fanart_betterposters_enable
import nuvio.composeapp.generated.resources.settings_fanart_betterposters_enable_description
import nuvio.composeapp.generated.resources.settings_fanart_betterposters_template_description
import nuvio.composeapp.generated.resources.settings_fanart_betterposters_template_placeholder
import nuvio.composeapp.generated.resources.settings_fanart_betterposters_template_title
import nuvio.composeapp.generated.resources.settings_fanart_clearlogos
import nuvio.composeapp.generated.resources.settings_fanart_clearlogos_description
import nuvio.composeapp.generated.resources.settings_fanart_enable
import nuvio.composeapp.generated.resources.settings_fanart_enable_description
import nuvio.composeapp.generated.resources.settings_fanart_posters
import nuvio.composeapp.generated.resources.settings_fanart_posters_description
import nuvio.composeapp.generated.resources.settings_fanart_prefer_english
import nuvio.composeapp.generated.resources.settings_fanart_prefer_english_description
import nuvio.composeapp.generated.resources.settings_fanart_section_api_key
import nuvio.composeapp.generated.resources.settings_fanart_section_betterposters
import nuvio.composeapp.generated.resources.settings_fanart_section_options
import nuvio.composeapp.generated.resources.settings_fanart_section_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.fanartSettingsContent(
    isTablet: Boolean,
    settings: FanartSettings,
) {
    val controlsEnabled = settings.enabled && settings.hasApiKey

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_fanart_section_title),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_enable),
                    description = stringResource(Res.string.settings_fanart_enable_description),
                    checked = settings.enabled,
                    enabled = settings.hasApiKey,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setEnabled,
                )
                if (!settings.hasApiKey) {
                    SettingsGroupDivider(isTablet = isTablet)
                    FanartInfoRow(
                        isTablet = isTablet,
                        text = stringResource(Res.string.settings_fanart_add_api_key_first),
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_fanart_section_api_key),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                FanartApiKeyRow(
                    isTablet = isTablet,
                    value = settings.apiKey,
                    onApiKeyCommitted = FanartSettingsRepository::setApiKey,
                )
            }
        }
    }

    item {
        var showQualityDialog by rememberSaveable { mutableStateOf(false) }

        if (showQualityDialog) {
            FanartQualityDialog(
                selected = settings.quality,
                onSelect = FanartSettingsRepository::setQuality,
                onDismiss = { showQualityDialog = false },
            )
        }

        SettingsSection(
            title = stringResource(Res.string.settings_fanart_section_options),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "Artwork Quality",
                    description = settings.quality.label,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onClick = { showQualityDialog = true },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = "Prefer HD Clear Logos",
                    description = "Prioritize lossless 1080p HDTV and HD Movie logos over standard clearlogos",
                    checked = settings.preferHdLogos,
                    enabled = controlsEnabled && settings.useClearLogos,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setPreferHdLogos,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = "Prefer HD ClearArt",
                    description = "Prioritize high-definition ClearArt artwork when available",
                    checked = settings.preferHdClearArt,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setPreferHdClearArt,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_clearlogos),
                    description = stringResource(Res.string.settings_fanart_clearlogos_description),
                    checked = settings.useClearLogos,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setUseClearLogos,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_prefer_english),
                    description = stringResource(Res.string.settings_fanart_prefer_english_description),
                    checked = settings.preferEnglishLogos,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setPreferEnglishLogos,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_backdrops),
                    description = stringResource(Res.string.settings_fanart_backdrops_description),
                    checked = settings.useHeroBackdrops,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setUseHeroBackdrops,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_banners),
                    description = stringResource(Res.string.settings_fanart_banners_description),
                    checked = settings.useBanners,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setUseBanners,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_posters),
                    description = stringResource(Res.string.settings_fanart_posters_description),
                    checked = settings.usePosters,
                    enabled = controlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setUsePosters,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_fanart_section_betterposters),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_fanart_betterposters_enable),
                    description = stringResource(Res.string.settings_fanart_betterposters_enable_description),
                    checked = settings.useBetterPosters,
                    enabled = true,
                    isTablet = isTablet,
                    onCheckedChange = FanartSettingsRepository::setUseBetterPosters,
                )
                if (settings.useBetterPosters) {
                    SettingsGroupDivider(isTablet = isTablet)
                    BetterPostersTemplateRow(
                        isTablet = isTablet,
                        value = settings.betterPostersTemplate,
                        onTemplateCommitted = FanartSettingsRepository::setBetterPostersTemplate,
                    )
                }
            }
        }
    }
}

@Composable
private fun FanartApiKeyRow(
    isTablet: Boolean,
    value: String,
    onApiKeyCommitted: (String) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    val normalizedDraft = draft.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.settings_fanart_api_key_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.settings_fanart_api_key_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSecretTextField(
            value = draft,
            onValueChange = {
                draft = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.settings_fanart_api_key_title),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    draft = normalizedDraft
                    onApiKeyCommitted(normalizedDraft)
                },
                enabled = normalizedDraft != value,
            ) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
}

@Composable
private fun BetterPostersTemplateRow(
    isTablet: Boolean,
    value: String,
    onTemplateCommitted: (String) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    val normalizedDraft = draft.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.settings_fanart_betterposters_template_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.settings_fanart_betterposters_template_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(Res.string.settings_fanart_betterposters_template_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    draft = normalizedDraft
                    onTemplateCommitted(normalizedDraft)
                },
                enabled = normalizedDraft != value,
            ) {
                Text(stringResource(Res.string.action_save))
            }
        }
    }
}

@Composable
private fun FanartInfoRow(
    isTablet: Boolean,
    text: String,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 14.dp else 12.dp

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FanartQualityDialog(
    selected: FanartArtworkQuality,
    onSelect: (FanartArtworkQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Artwork Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Choose the download quality for Fanart.tv posters, backdrops, and banners.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FanartArtworkQuality.entries.forEach { q ->
                        val isSelected = q == selected
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(q)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = q.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = q.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
