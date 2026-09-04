package com.nuvio.app.features.anilist.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import com.nuvio.app.core.ui.nuvioHorizontalScroll
import com.nuvio.app.features.anilist.AnilistAdvancedFilterState
import com.nuvio.app.features.anilist.AnilistGenres
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.anilist.AnilistSortOption
import kotlin.math.roundToInt

private val AniHyouCyanAccent = Color(0xFF40C4FF)
private val AniHyouCardBg = Color(0xFF16181F)
private val AniHyouChipBg = Color(0xFF1F222B)
private val AniHyouChipActive = Color(0xFF2C3442)
private val AniHyouBorder = Color(0xFF2A2E3B)
private val AniHyouSubtle = Color(0xFF8E92A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniHyouSearchFilterSheet(
    initialFilter: AnilistAdvancedFilterState,
    initialQuery: String = "",
    isDesktop: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (AnilistAdvancedFilterState, String) -> Unit,
) {
    var state by remember { mutableStateOf(initialFilter) }
    var searchQuery by remember { mutableStateOf(initialQuery) }

    if (isDesktop) {
        Dialog(onDismissRequest = {
            onApply(state, searchQuery)
            onDismiss()
        }) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 680.dp, max = 840.dp)
                    .heightIn(max = 760.dp)
                    .clip(RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, AniHyouBorder),
            ) {
                AniHyouSearchFilterContent(
                    state = state,
                    searchQuery = searchQuery,
                    onStateChange = { updated ->
                        state = updated
                        onApply(updated, searchQuery)
                    },
                    onSearchQueryChange = { newQ ->
                        searchQuery = newQ
                        onApply(state, newQ)
                    },
                    onReset = {
                        val resetState = state.reset()
                        state = resetState
                        searchQuery = ""
                        onApply(resetState, "")
                    },
                    onApply = {
                        onApply(state, searchQuery)
                        onDismiss()
                    },
                    onClose = {
                        onApply(state, searchQuery)
                        onDismiss()
                    },
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                onApply(state, searchQuery)
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            AniHyouSearchFilterContent(
                state = state,
                searchQuery = searchQuery,
                onStateChange = { updated ->
                    state = updated
                    onApply(updated, searchQuery)
                },
                onSearchQueryChange = { newQ ->
                    searchQuery = newQ
                    onApply(state, newQ)
                },
                onReset = {
                    val resetState = state.reset()
                    state = resetState
                    searchQuery = ""
                    onApply(resetState, "")
                },
                onApply = {
                    onApply(state, searchQuery)
                    onDismiss()
                },
                onClose = {
                    onApply(state, searchQuery)
                    onDismiss()
                },
                modifier = Modifier.fillMaxHeight(0.92f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AniHyouSearchFilterContent(
    state: AnilistAdvancedFilterState,
    searchQuery: String,
    onStateChange: (AnilistAdvancedFilterState) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val hideAdult = AnilistPreferencesRepository.snapshot().hideAdultContent
    val availableGenres = remember(hideAdult) { AnilistGenres.getAvailableGenres(hideAdult) }

    var showGenreDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showEpisodesDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Search Bar (Back button + Text Input + Clear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (state.onMyList == true) "Search my list..." else "Anime, manga and more...",
                        color = AniHyouSubtle,
                        fontSize = 15.sp,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear text", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Row 1: Search Type chips [Anime] [Manga] [Characters] [Staff]
                item {
                    val searchTypeScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nuvioHorizontalScroll(searchTypeScroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("ANIME" to "Anime", "MANGA" to "Manga", "CHARACTERS" to "Characters", "STAFF" to "Staff").forEach { (typeKey, label) ->
                            val isSelected = state.searchType.equals(typeKey, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStateChange(state.copy(searchType = typeKey)) },
                                label = { Text(text = label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AniHyouChipActive,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                }

                // Row 2: Sort dropdown chip [≡ Sort ▼]
                item {
                    var sortMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = state.sort != AnilistSortOption.POPULARITY,
                            onClick = { sortMenuOpen = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            label = { Text(text = state.sort.label) },
                        )
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                        ) {
                            AnilistSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option.label) },
                                    onClick = {
                                        onStateChange(state.copy(sort = option))
                                        sortMenuOpen = false
                                    },
                                    leadingIcon = if (state.sort == option) {
                                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                }

                // Row 3: [Format] [Status] [On my list] [Country]
                item {
                    val row3Scroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nuvioHorizontalScroll(row3Scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Format chip
                        FilterChip(
                            selected = state.formats.isNotEmpty(),
                            onClick = { showFormatDialog = true },
                            label = {
                                Text(text = if (state.formats.isEmpty()) "Format" else "Format (${state.formats.size})")
                            },
                        )

                        // Status chip
                        FilterChip(
                            selected = state.statuses.isNotEmpty(),
                            onClick = { showStatusDialog = true },
                            label = {
                                Text(text = if (state.statuses.isEmpty()) "Status" else "Status (${state.statuses.size})")
                            },
                        )

                        // On my list tri-state chip (null -> true -> false -> null)
                        FilterChip(
                            selected = state.onMyList != null,
                            onClick = {
                                val next = when (state.onMyList) {
                                    null -> true
                                    true -> false
                                    false -> null
                                }
                                onStateChange(state.copy(onMyList = next))
                            },
                            leadingIcon = when (state.onMyList) {
                                true -> { { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                                false -> { { Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) } }
                                null -> null
                            },
                            label = {
                                Text(
                                    text = when (state.onMyList) {
                                        true -> "On my list"
                                        false -> "Not on list"
                                        null -> "On my list"
                                    }
                                )
                            },
                        )

                        // Country chip
                        var countryMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = !state.countryOfOrigin.isNullOrBlank(),
                                onClick = { countryMenuOpen = true },
                                label = {
                                    val label = when (state.countryOfOrigin) {
                                        "JP" -> "Japan (JP)"
                                        "KR" -> "South Korea (KR)"
                                        "CN" -> "China (CN)"
                                        "TW" -> "Taiwan (TW)"
                                        else -> "Country"
                                    }
                                    Text(text = label)
                                },
                            )
                            DropdownMenu(
                                expanded = countryMenuOpen,
                                onDismissRequest = { countryMenuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any Country") },
                                    onClick = {
                                        onStateChange(state.copy(countryOfOrigin = null))
                                        countryMenuOpen = false
                                    },
                                )
                                listOf("JP" to "Japan (JP)", "KR" to "South Korea (KR)", "CN" to "China (CN)", "TW" to "Taiwan (TW)").forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onStateChange(state.copy(countryOfOrigin = code))
                                            countryMenuOpen = false
                                        },
                                        leadingIcon = if (state.countryOfOrigin == code) {
                                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 4: [From year] - [To year] [Season]
                item {
                    val row4Scroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nuvioHorizontalScroll(row4Scroll),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        var fromYearMenu by remember { mutableStateOf(false) }
                        var toYearMenu by remember { mutableStateOf(false) }
                        var seasonMenu by remember { mutableStateOf(false) }

                        val years = remember { (2026 downTo 1970).toList() }

                        // From year
                        Box {
                            FilterChip(
                                selected = state.fromYear != null,
                                onClick = { fromYearMenu = true },
                                label = { Text(text = state.fromYear?.toString() ?: "From year") },
                            )
                            DropdownMenu(
                                expanded = fromYearMenu,
                                onDismissRequest = { fromYearMenu = false },
                                modifier = Modifier.heightIn(max = 280.dp),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any") },
                                    onClick = {
                                        onStateChange(state.copy(fromYear = null))
                                        fromYearMenu = false
                                    },
                                )
                                years.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr.toString()) },
                                        onClick = {
                                            onStateChange(state.copy(fromYear = yr))
                                            fromYearMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        Text(text = " - ", color = AniHyouSubtle, fontWeight = FontWeight.Bold)

                        // To year
                        Box {
                            FilterChip(
                                selected = state.toYear != null,
                                onClick = { toYearMenu = true },
                                label = { Text(text = state.toYear?.toString() ?: "To year") },
                            )
                            DropdownMenu(
                                expanded = toYearMenu,
                                onDismissRequest = { toYearMenu = false },
                                modifier = Modifier.heightIn(max = 280.dp),
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any") },
                                    onClick = {
                                        onStateChange(state.copy(toYear = null))
                                        toYearMenu = false
                                    },
                                )
                                years.filter { state.fromYear == null || it >= state.fromYear!! }.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr.toString()) },
                                        onClick = {
                                            onStateChange(state.copy(toYear = yr))
                                            toYearMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        // Season chip
                        Box {
                            FilterChip(
                                selected = !state.season.isNullOrBlank(),
                                onClick = { seasonMenu = true },
                                label = {
                                    val s = when (state.season?.uppercase()) {
                                        "WINTER" -> "Winter ❄️"
                                        "SPRING" -> "Spring 🌸"
                                        "SUMMER" -> "Summer ☀️"
                                        "FALL" -> "Fall 🍂"
                                        else -> "Season"
                                    }
                                    Text(text = s)
                                },
                            )
                            DropdownMenu(
                                expanded = seasonMenu,
                                onDismissRequest = { seasonMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any Season") },
                                    onClick = {
                                        onStateChange(state.copy(season = null))
                                        seasonMenu = false
                                    },
                                )
                                listOf("WINTER" to "Winter ❄️", "SPRING" to "Spring 🌸", "SUMMER" to "Summer ☀️", "FALL" to "Fall 🍂").forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onStateChange(state.copy(season = code))
                                            seasonMenu = false
                                        },
                                        leadingIcon = if (state.season.equals(code, ignoreCase = true)) {
                                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 5: [▷ Episodes] [⏱ Duration]
                item {
                    val row5Scroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nuvioHorizontalScroll(row5Scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val hasEpisodes = state.minEpisodes != null || state.maxEpisodes != null
                        FilterChip(
                            selected = hasEpisodes,
                            onClick = { showEpisodesDialog = true },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                val label = if (hasEpisodes) {
                                    "${state.minEpisodes ?: 0} - ${state.maxEpisodes ?: "150+"} eps"
                                } else {
                                    "Episodes"
                                }
                                Text(text = label)
                            },
                        )

                        val hasDuration = state.minDuration != null || state.maxDuration != null
                        FilterChip(
                            selected = hasDuration,
                            onClick = { showDurationDialog = true },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                val label = if (hasDuration) {
                                    "${state.minDuration ?: 0} - ${state.maxDuration ?: "170+"} min"
                                } else {
                                    "Duration"
                                }
                                Text(text = label)
                            },
                        )
                    }
                }

                // Row 6: [+ Add genre] and active genre chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = { showGenreDialog = true },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Add genre") },
                            )

                            if (state.includedGenres.isNotEmpty() || state.excludedGenres.isNotEmpty()) {
                                Text(
                                    text = "${state.includedGenres.size + state.excludedGenres.size} selected",
                                    fontSize = 12.sp,
                                    color = AniHyouSubtle,
                                )
                            }
                        }

                        // Flow of selected genres
                        if (state.includedGenres.isNotEmpty() || state.excludedGenres.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                state.includedGenres.forEach { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AniHyouCyanAccent.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, AniHyouCyanAccent),
                                        modifier = Modifier.clickable {
                                            // Toggle to excluded
                                            onStateChange(state.copy(
                                                includedGenres = state.includedGenres - genre,
                                                excludedGenres = state.excludedGenres + genre,
                                            ))
                                        },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(text = "+ $genre", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AniHyouCyanAccent)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { onStateChange(state.copy(includedGenres = state.includedGenres - genre)) },
                                                tint = AniHyouCyanAccent,
                                            )
                                        }
                                    }
                                }
                                state.excludedGenres.forEach { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        modifier = Modifier.clickable {
                                            // Remove
                                            onStateChange(state.copy(excludedGenres = state.excludedGenres - genre))
                                        },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(text = "- $genre", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { onStateChange(state.copy(excludedGenres = state.excludedGenres - genre)) },
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Row 7: [Doujinshi] [Adult] toggles
                item {
                    val row7Scroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nuvioHorizontalScroll(row7Scroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.isDoujin == true,
                            onClick = {
                                onStateChange(state.copy(isDoujin = if (state.isDoujin == true) null else true))
                            },
                            label = { Text("Doujinshi") },
                        )

                        FilterChip(
                            selected = state.isAdult == true,
                            onClick = {
                                onStateChange(state.copy(isAdult = if (state.isAdult == true) null else true))
                            },
                            label = { Text("Adult") },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            NuvioDesktopVerticalScrollbar(
                state = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }

        // Bottom Action Bar: [Hide filters] [Clear] [Apply]
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AniHyouBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onClose) {
                    Text(text = "Hide filters", color = MaterialTheme.colorScheme.onSurface)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.hasFilters || searchQuery.isNotBlank()) {
                        TextButton(onClick = {
                            onReset()
                            onSearchQueryChange("")
                        }) {
                            Text(text = "Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onApply,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AniHyouCyanAccent,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(
                            text = if (state.activeFilterCount > 0) "Apply (${state.activeFilterCount})" else "Apply",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // Modal dialog for selecting formats
    if (showFormatDialog) {
        val formatList = listOf(
            "TV" to "TV Series",
            "TV_SHORT" to "TV Short",
            "MOVIE" to "Movie",
            "SPECIAL" to "Special",
            "OVA" to "OVA",
            "ONA" to "ONA",
            "MUSIC" to "Music",
        )
        MultiSelectDialog(
            title = "Select Formats",
            items = formatList,
            selectedKeys = state.formats,
            onDismiss = { showFormatDialog = false },
            onConfirm = { updated ->
                onStateChange(state.copy(formats = updated))
                showFormatDialog = false
            },
        )
    }

    // Modal dialog for selecting statuses
    if (showStatusDialog) {
        val statusList = listOf(
            "RELEASING" to "Airing",
            "FINISHED" to "Finished",
            "NOT_YET_RELEASED" to "Not Yet Aired",
            "CANCELLED" to "Cancelled",
            "HIATUS" to "Paused",
        )
        MultiSelectDialog(
            title = "Select Status",
            items = statusList,
            selectedKeys = state.statuses,
            onDismiss = { showStatusDialog = false },
            onConfirm = { updated ->
                onStateChange(state.copy(statuses = updated))
                showStatusDialog = false
            },
        )
    }

    // Episodes Range Dialog
    if (showEpisodesDialog) {
        RangeSliderDialog(
            title = "Episodes Range",
            minValue = 0f,
            maxValue = 150f,
            currentMin = state.minEpisodes?.toFloat() ?: 0f,
            currentMax = state.maxEpisodes?.toFloat() ?: 150f,
            onDismiss = { showEpisodesDialog = false },
            onConfirm = { min, max ->
                onStateChange(state.copy(
                    minEpisodes = if (min > 0f) min.roundToInt() else null,
                    maxEpisodes = if (max < 150f) max.roundToInt() else null,
                ))
                showEpisodesDialog = false
            },
        )
    }

    // Duration Range Dialog
    if (showDurationDialog) {
        RangeSliderDialog(
            title = "Duration Range (Minutes)",
            minValue = 0f,
            maxValue = 170f,
            currentMin = state.minDuration?.toFloat() ?: 0f,
            currentMax = state.maxDuration?.toFloat() ?: 170f,
            onDismiss = { showDurationDialog = false },
            onConfirm = { min, max ->
                onStateChange(state.copy(
                    minDuration = if (min > 0f) min.roundToInt() else null,
                    maxDuration = if (max < 170f) max.roundToInt() else null,
                ))
                showDurationDialog = false
            },
        )
    }

    // Genre Selector Dialog with tri-state selection (+ included, - excluded)
    if (showGenreDialog) {
        GenreSelectorDialog(
            availableGenres = availableGenres,
            included = state.includedGenres,
            excluded = state.excludedGenres,
            onDismiss = { showGenreDialog = false },
            onConfirm = { inc, exc ->
                onStateChange(state.copy(includedGenres = inc, excludedGenres = exc))
                showGenreDialog = false
            },
        )
    }
}

@Composable
private fun MultiSelectDialog(
    title: String,
    items: List<Pair<String, String>>,
    selectedKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(selectedKeys) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AniHyouBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                Column(modifier = Modifier.fillMaxWidth()) {
                    items.forEach { (key, label) ->
                        val isChecked = selected.contains(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isChecked) selected - key else selected + key
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { chk ->
                                    selected = if (chk) selected + key else selected - key
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AniHyouCyanAccent),
                            )
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(selected) },
                        colors = ButtonDefaults.buttonColors(containerColor = AniHyouCyanAccent, contentColor = Color.Black),
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSliderDialog(
    title: String,
    minValue: Float,
    maxValue: Float,
    currentMin: Float,
    currentMax: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float) -> Unit,
) {
    var start by remember { mutableFloatStateOf(currentMin) }
    var end by remember { mutableFloatStateOf(currentMax) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AniHyouBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                Text(
                    text = "${start.roundToInt()} - ${if (end >= maxValue) "${maxValue.roundToInt()}+" else end.roundToInt().toString()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AniHyouCyanAccent,
                    fontWeight = FontWeight.Bold,
                )

                RangeSlider(
                    value = start..end,
                    onValueChange = { range ->
                        start = range.start
                        end = range.endInclusive
                    },
                    valueRange = minValue..maxValue,
                    colors = SliderDefaults.colors(
                        thumbColor = AniHyouCyanAccent,
                        activeTrackColor = AniHyouCyanAccent,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(start, end) },
                        colors = ButtonDefaults.buttonColors(containerColor = AniHyouCyanAccent, contentColor = Color.Black),
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreSelectorDialog(
    availableGenres: List<String>,
    included: Set<String>,
    excluded: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>, Set<String>) -> Unit,
) {
    var inc by remember { mutableStateOf(included) }
    var exc by remember { mutableStateOf(excluded) }
    var filterText by remember { mutableStateOf("") }

    val filtered = remember(filterText, availableGenres) {
        if (filterText.isBlank()) availableGenres
        else availableGenres.filter { it.contains(filterText, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AniHyouBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Genres & Tags", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Tap once to Include (+), tap again to Exclude (-), tap again to remove.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AniHyouSubtle,
                )

                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Filter genres...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                val scrollState = rememberLazyListState()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                filtered.forEach { genre ->
                                    val isInc = inc.contains(genre)
                                    val isExc = exc.contains(genre)

                                    val chipColor = when {
                                        isInc -> AniHyouCyanAccent.copy(alpha = 0.25f)
                                        isExc -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                        else -> AniHyouChipBg
                                    }
                                    val borderColor = when {
                                        isInc -> AniHyouCyanAccent
                                        isExc -> MaterialTheme.colorScheme.error
                                        else -> AniHyouBorder
                                    }
                                    val textColor = when {
                                        isInc -> AniHyouCyanAccent
                                        isExc -> MaterialTheme.colorScheme.error
                                        else -> Color.White
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = chipColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier.clickable {
                                            when {
                                                isInc -> {
                                                    inc = inc - genre
                                                    exc = exc + genre
                                                }
                                                isExc -> {
                                                    exc = exc - genre
                                                }
                                                else -> {
                                                    inc = inc + genre
                                                }
                                            }
                                        },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = when {
                                                    isInc -> "+ $genre"
                                                    isExc -> "- $genre"
                                                    else -> genre
                                                },
                                                color = textColor,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    NuvioDesktopVerticalScrollbar(
                        state = scrollState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(inc, exc) },
                        colors = ButtonDefaults.buttonColors(containerColor = AniHyouCyanAccent, contentColor = Color.Black),
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
