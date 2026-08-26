package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import nuvio.composeapp.generated.resources.settings_fanart_clearlogos
import nuvio.composeapp.generated.resources.settings_fanart_clearlogos_description
import nuvio.composeapp.generated.resources.settings_fanart_enable
import nuvio.composeapp.generated.resources.settings_fanart_enable_description
import nuvio.composeapp.generated.resources.settings_fanart_posters
import nuvio.composeapp.generated.resources.settings_fanart_posters_description
import nuvio.composeapp.generated.resources.settings_fanart_prefer_english
import nuvio.composeapp.generated.resources.settings_fanart_prefer_english_description
import nuvio.composeapp.generated.resources.settings_fanart_section_api_key
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
        SettingsSection(
            title = stringResource(Res.string.settings_fanart_section_options),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
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
