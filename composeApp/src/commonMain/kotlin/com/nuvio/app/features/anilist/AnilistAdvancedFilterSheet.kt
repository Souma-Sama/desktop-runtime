package com.nuvio.app.features.anilist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nuvio.app.core.ui.NuvioDesktopVerticalScrollbar
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnilistAdvancedFilterSheet(
    initialFilter: AnilistAdvancedFilterState,
    isDesktop: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (AnilistAdvancedFilterState) -> Unit,
) {
    var state by remember { mutableStateOf(initialFilter) }
    var tagSearchQuery by remember { mutableStateOf("") }

    if (isDesktop) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 680.dp, max = 840.dp)
                    .heightIn(max = 760.dp)
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            ) {
                FilterContent(
                    state = state,
                    tagSearchQuery = tagSearchQuery,
                    onStateChange = { state = it },
                    onTagSearchChange = { tagSearchQuery = it },
                    onReset = { state = AnilistAdvancedFilterState(sort = state.sort) },
                    onApply = {
                        onApply(state)
                        onDismiss()
                    },
                    onClose = onDismiss,
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            FilterContent(
                state = state,
                tagSearchQuery = tagSearchQuery,
                onStateChange = { state = it },
                onTagSearchChange = { tagSearchQuery = it },
                onReset = { state = AnilistAdvancedFilterState(sort = state.sort) },
                onApply = {
                    onApply(state)
                    onDismiss()
                },
                onClose = onDismiss,
                modifier = Modifier.fillMaxHeight(0.88f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterContent(
    state: AnilistAdvancedFilterState,
    tagSearchQuery: String,
    onStateChange: (AnilistAdvancedFilterState) -> Unit,
    onTagSearchChange: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val hideAdult = AnilistPreferencesRepository.snapshot().hideAdultContent
    val availableGenres = remember(hideAdult) { AnilistGenres.getAvailableGenres(hideAdult) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Advanced Anime Filters",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.hasFilters) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "${state.activeFilterCount}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.hasFilters) {
                    TextButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 1. Genres Section (Tri-state chips: Normal -> Included (+) -> Excluded (-))
                item {
                    FilterSectionHeader(
                        title = "Genres",
                        subtitle = "Tap once to Include (+), tap again to Exclude (-)",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        availableGenres.forEach { genre ->
                            val isIncluded = state.includedGenres.contains(genre)
                            val isExcluded = state.excludedGenres.contains(genre)
                            TriStateChip(
                                label = genre,
                                isIncluded = isIncluded,
                                isExcluded = isExcluded,
                                onClick = {
                                    onStateChange(
                                        when {
                                            isIncluded -> state.copy(
                                                includedGenres = state.includedGenres - genre,
                                                excludedGenres = state.excludedGenres + genre,
                                            )
                                            isExcluded -> state.copy(
                                                excludedGenres = state.excludedGenres - genre,
                                            )
                                            else -> state.copy(
                                                includedGenres = state.includedGenres + genre,
                                            )
                                        }
                                    )
                                },
                            )
                        }
                    }
                }

                // 2. Official Tags Section
                item {
                    FilterSectionHeader(
                        title = "Tags & Themes",
                        subtitle = "Select from 50+ official AniList tags",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagSearchQuery,
                        onValueChange = onTagSearchChange,
                        placeholder = { Text("Search tags (e.g. Isekai, Time Travel, Magic)...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (tagSearchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { onTagSearchChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredTags = remember(tagSearchQuery) {
                        if (tagSearchQuery.isBlank()) AnilistGenres.POPULAR_TAGS
                        else AnilistGenres.POPULAR_TAGS.filter { it.contains(tagSearchQuery.trim(), ignoreCase = true) }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        filteredTags.forEach { tag ->
                            val isIncluded = state.includedTags.contains(tag)
                            val isExcluded = state.excludedTags.contains(tag)
                            TriStateChip(
                                label = tag,
                                isIncluded = isIncluded,
                                isExcluded = isExcluded,
                                onClick = {
                                    onStateChange(
                                        when {
                                            isIncluded -> state.copy(
                                                includedTags = state.includedTags - tag,
                                                excludedTags = state.excludedTags + tag,
                                            )
                                            isExcluded -> state.copy(
                                                excludedTags = state.excludedTags - tag,
                                            )
                                            else -> state.copy(
                                                includedTags = state.includedTags + tag,
                                            )
                                        }
                                    )
                                },
                            )
                        }
                    }
                }

                // 3. Format Section
                item {
                    FilterSectionHeader(title = "Format")
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnilistGenres.FORMAT_OPTIONS.forEach { (key, label) ->
                            val isSelected = state.formats.contains(key)
                            SelectableChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    onStateChange(
                                        if (isSelected) state.copy(formats = state.formats - key)
                                        else state.copy(formats = state.formats + key)
                                    )
                                },
                            )
                        }
                    }
                }

                // 4. Airing Status Section
                item {
                    FilterSectionHeader(title = "Airing Status")
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnilistGenres.STATUS_OPTIONS.forEach { (key, label) ->
                            val isSelected = state.statuses.contains(key)
                            SelectableChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    onStateChange(
                                        if (isSelected) state.copy(statuses = state.statuses - key)
                                        else state.copy(statuses = state.statuses + key)
                                    )
                                },
                            )
                        }
                    }
                }

                // 5. Country of Origin
                item {
                    FilterSectionHeader(title = "Country of Origin")
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnilistGenres.COUNTRY_OPTIONS.forEach { (code, label) ->
                            val isSelected = state.countryOfOrigin == code
                            SelectableChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    onStateChange(state.copy(countryOfOrigin = if (isSelected) null else code))
                                },
                            )
                        }
                    }
                }

                // 6. Source Material
                item {
                    FilterSectionHeader(title = "Source Material")
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnilistGenres.SOURCE_OPTIONS.forEach { (key, label) ->
                            val isSelected = state.sources.contains(key)
                            SelectableChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    onStateChange(
                                        if (isSelected) state.copy(sources = state.sources - key)
                                        else state.copy(sources = state.sources + key)
                                    )
                                },
                            )
                        }
                    }
                }

                // 7. Season & Year
                item {
                    FilterSectionHeader(title = "Season & Year")
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AnilistGenres.SEASON_OPTIONS.forEach { (code, label) ->
                            val isSelected = state.season == code
                            SelectableChip(
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    onStateChange(state.copy(season = if (isSelected) null else code))
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentYear = 2026
                    val years = remember { (currentYear downTo 1980).toList() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NuvioDropdownChip(
                            title = "Select Year",
                            label = state.seasonYear?.toString() ?: "All Years",
                            selectedKey = state.seasonYear?.toString() ?: "",
                            options = listOf(NuvioDropdownOption("", "All Years")) + years.map { NuvioDropdownOption(it.toString(), it.toString()) },
                            onSelected = { opt ->
                                onStateChange(state.copy(seasonYear = opt.key.toIntOrNull()))
                            },
                        )
                    }
                }

                // 8. Minimum Average Score
                item {
                    val currentScore = (state.minScore ?: 0).toFloat()
                    FilterSectionHeader(
                        title = "Minimum Score",
                        subtitle = if (currentScore > 0) "≥ ${currentScore.toInt()}%" else "Any Score",
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = currentScore,
                        onValueChange = { onStateChange(state.copy(minScore = if (it > 0f) it.roundToInt() else null)) },
                        valueRange = 0f..95f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                // 9. Sorting
                item {
                    FilterSectionHeader(title = "Sort Order")
                    Spacer(modifier = Modifier.height(8.dp))
                    NuvioDropdownChip(
                        title = "Sort By",
                        label = state.sort.label,
                        selectedKey = state.sort.name,
                        options = AnilistSortOption.entries.map { NuvioDropdownOption(it.name, it.label) },
                        onSelected = { opt ->
                            val parsed = AnilistSortOption.fromLabelOrNull(opt.key) ?: AnilistSortOption.POPULARITY
                            onStateChange(state.copy(sort = parsed))
                        },
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            NuvioDesktopVerticalScrollbar(
                listState = listState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.hasFilters) "Apply Filters (${state.activeFilterCount})" else "Apply",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(
    title: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TriStateChip(
    label: String,
    isIncluded: Boolean,
    isExcluded: Boolean,
    onClick: () -> Unit,
) {
    val (bgColor, textColor, borderColor) = when {
        isIncluded -> Triple(
            Color(0xFF1B5E20).copy(alpha = 0.22f),
            Color(0xFF81C784),
            Color(0xFF4CAF50),
        )
        isExcluded -> Triple(
            Color(0xFFB71C1C).copy(alpha = 0.22f),
            Color(0xFFE57373),
            Color(0xFFF44336),
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isIncluded) {
                Icon(Icons.Default.Add, contentDescription = "Included", tint = textColor, modifier = Modifier.size(12.dp))
            } else if (isExcluded) {
                Icon(Icons.Default.Remove, contentDescription = "Excluded", tint = textColor, modifier = Modifier.size(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isIncluded || isExcluded) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                ),
                color = textColor,
            )
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                ),
                color = textColor,
            )
        }
    }
}
