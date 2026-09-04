package com.nuvio.app.features.anilist.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.nuvio.app.core.ui.LocalNuvioFloatingSidebarPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.time.EpisodeReleaseDatePlatform
import com.nuvio.app.core.ui.FluidSlidingSegmentedBar
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.cardWidth
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.anilist.AnilistAdvancedFilterSheet
import com.nuvio.app.features.anilist.AnilistAdvancedFilterState
import com.nuvio.app.features.anilist.AnilistGenres
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.HomePosterCard
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(
    onAnimeClick: (MetaPreview) -> Unit,
    onAnimeLongClick: ((MetaPreview) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topChromePadding: Dp = 0.dp,
) {
    val state by CalendarRepository.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showAdvancedFilterSheet by remember { mutableStateOf(false) }
    var advancedFilterState by remember { mutableStateOf(AnilistAdvancedFilterState()) }

    LaunchedEffect(Unit) {
        CalendarRepository.loadWeeklySchedule()
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalTopPadding = maxOf(topChromePadding, statusBarPadding + 14.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // AniHyou-style Top Bar
        CalendarHeader(
            onBack = onBack,
            onMyList = state.onMyList,
            onMyListChanged = { CalendarRepository.setOnMyList(it) },
            onRefresh = {
                scope.launch {
                    CalendarRepository.loadWeeklySchedule(force = true)
                }
            },
            topPadding = totalTopPadding,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Weekday Tabs (Monday - Sunday)
        CalendarDayTabs(
            selectedDay = state.selectedDay,
            scheduleItems = state.scheduleItems,
            onDaySelected = { CalendarRepository.setDay(it) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary Genre, Sort & Filter Bar
        CalendarFilterBar(
            selectedGenre = state.selectedGenre,
            selectedSort = state.selectedSort,
            onMyList = state.onMyList,
            onGenreSelected = { CalendarRepository.setGenreFilter(it) },
            onSortSelected = { CalendarRepository.setSortOption(it) },
            onMyListChanged = { CalendarRepository.setOnMyList(it) },
            onOpenFilter = { showAdvancedFilterSheet = true },
            activeFilterCount = advancedFilterState.activeFilterCount,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading && state.scheduleItems.isEmpty()) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            } else if (state.errorMessage != null && state.scheduleItems.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = state.errorMessage ?: "Unable to load airing schedule",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                scope.launch {
                                    CalendarRepository.loadWeeklySchedule(force = true)
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            } else {
                val dayItems = remember(
                    state.scheduleItems,
                    state.selectedDay,
                    state.selectedGenre,
                    state.selectedSort,
                    state.onMyList,
                ) {
                    var list = state.scheduleItems[state.selectedDay].orEmpty()
                    when (state.onMyList) {
                        true -> list = list.filter { it.onMyList }
                        false -> list = list.filter { !it.onMyList }
                        null -> {}
                    }
                    if (!state.selectedGenre.isNullOrBlank()) {
                        list = list.filter { it.genres.any { g -> g.equals(state.selectedGenre, ignoreCase = true) } }
                    }
                    when (state.selectedSort) {
                        CalendarSort.AIRING_TIME -> list.sortedBy { it.airingAt ?: Long.MAX_VALUE }
                        CalendarSort.POPULARITY -> list.sortedByDescending { it.popularity ?: 0 }
                        CalendarSort.SCORE -> list.sortedByDescending { it.score ?: 0.0 }
                        CalendarSort.TITLE -> list.sortedBy { it.title.lowercase() }
                        CalendarSort.EPISODES -> list.sortedByDescending { it.episodes ?: 0 }
                    }
                }

                CalendarScheduleGrid(
                    items = dayItems,
                    onMyListActive = state.onMyList != null,
                    onAnimeClick = onAnimeClick,
                    onAnimeLongClick = onAnimeLongClick,
                )
            }
        }
    }

    if (showAdvancedFilterSheet) {
        AnilistAdvancedFilterSheet(
            initialFilter = advancedFilterState,
            isDesktop = true,
            onDismiss = { showAdvancedFilterSheet = false },
            onApply = { newFilter ->
                advancedFilterState = newFilter
                CalendarRepository.setGenreFilter(newFilter.includedGenres.firstOrNull())
                showAdvancedFilterSheet = false
            },
        )
    }
}

@Composable
private fun CalendarHeader(
    onBack: (() -> Unit)?,
    onMyList: Boolean?,
    onMyListChanged: (Boolean?) -> Unit,
    onRefresh: () -> Unit,
    topPadding: Dp,
) {
    var menuOpened by remember { mutableStateOf(false) }

    val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
    val startPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = startPadding, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Actions: 3-dots popup menu (1:1 AniHyou)
        Box {
            IconButton(
                onClick = { menuOpened = !menuOpened },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            DropdownMenu(
                expanded = menuOpened,
                onDismissRequest = { menuOpened = false },
                modifier = Modifier.background(Color(0xFF25262B)),
            ) {
                DropdownMenuItem(
                    text = { Text("On My List", color = Color.White) },
                    leadingIcon = {
                        when (onMyList) {
                            true -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "On My List",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            false -> Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Not On My List",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            null -> Spacer(modifier = Modifier.size(20.dp))
                        }
                    },
                    onClick = {
                        val next = when (onMyList) {
                            null -> true
                            true -> false
                            false -> null
                        }
                        onMyListChanged(next)
                        menuOpened = false
                    },
                )

                HorizontalDivider(color = Color(0xFF383A42))

                DropdownMenuItem(
                    text = { Text("Refresh", color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFC4C6D0),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        onRefresh()
                        menuOpened = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CalendarDayTabs(
    selectedDay: CalendarDay,
    scheduleItems: Map<CalendarDay, List<CalendarMedia>>,
    onDaySelected: (CalendarDay) -> Unit,
) {
    val today = remember { CalendarDay.today() }
    val dayScrollState = rememberScrollState()
    val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
    val startPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 20.dp

    Box(
        modifier = Modifier
            .nuvioHorizontalScrollBleed(20.dp)
            .fillMaxWidth()
            .horizontalScroll(dayScrollState)
            .nuvioDesktopDragScroll(dayScrollState)
            .padding(start = startPadding, end = 20.dp),
    ) {
        FluidSlidingSegmentedBar(
            items = CalendarDay.entries,
            selectedItem = selectedDay,
            onItemSelected = onDaySelected,
        ) { day, isSelected ->
            val isToday = day == today
            val count = scheduleItems[day]?.size ?: 0
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = contentColor,
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = if (count > 999) "${count / 1000}k" else "$count",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.20f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF10B981),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarFilterBar(
    selectedGenre: String?,
    selectedSort: CalendarSort,
    onMyList: Boolean?,
    onGenreSelected: (String?) -> Unit,
    onSortSelected: (CalendarSort) -> Unit,
    onMyListChanged: (Boolean?) -> Unit,
    onOpenFilter: () -> Unit,
    activeFilterCount: Int = 0,
) {
    val prefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()
    val genres = remember(prefs.hideAdultContent) {
        AnilistGenres.getAvailableGenres(prefs.hideAdultContent)
    }

    val genreOptions = remember(genres) {
        buildList {
            add(NuvioDropdownOption(key = "", label = "All Genres"))
            addAll(genres.map { NuvioDropdownOption(key = it, label = it) })
        }
    }

    val sortOptions = remember {
        listOf(
            CalendarSort.AIRING_TIME,
            CalendarSort.POPULARITY,
            CalendarSort.SCORE,
            CalendarSort.TITLE,
        ).map { NuvioDropdownOption(key = it.name, label = it.label) }
    }

    val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
    val startPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onMyList != null) {
            Surface(
                onClick = { onMyListChanged(null) },
                shape = RoundedCornerShape(8.dp),
                color = if (onMyList) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, if (onMyList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (onMyList) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (onMyList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = if (onMyList) "On My List" else "Not On List",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        NuvioDropdownChip(
            title = "Genre",
            label = selectedGenre ?: "All Genres",
            selectedKey = selectedGenre ?: "",
            options = genreOptions,
            enabled = true,
            onSelected = { option ->
                onGenreSelected(option.key.ifBlank { null })
            },
        )

        NuvioDropdownChip(
            title = "Sort",
            label = selectedSort.label,
            selectedKey = selectedSort.name,
            options = sortOptions,
            enabled = true,
            onSelected = { option ->
                CalendarSort.entries.find { it.name == option.key }?.let { onSortSelected(it) }
            },
        )

        val hasActive = activeFilterCount > 0
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenFilter() },
            shape = RoundedCornerShape(8.dp),
            color = if (hasActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, if (hasActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = if (hasActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = if (hasActive) "Filters ($activeFilterCount)" else "Filters",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = if (hasActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalendarScheduleGrid(
    items: List<CalendarMedia>,
    onMyListActive: Boolean,
    onAnimeClick: (MetaPreview) -> Unit,
    onAnimeLongClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (onMyListActive) "No anime on your list airing on this day" else "No scheduled anime broadcasts for this day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val posterCardStyle = rememberPosterCardStyleUiState()
    val baseWidth = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    val cardWidth = NuvioPosterShape.Poster.cardWidth(basePosterWidthDp = baseWidth)

    val gridState = rememberLazyGridState()
    val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
    val startPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 20.dp
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cardWidth),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = startPadding, end = 20.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = items,
            key = { "${it.id}-${it.airingAt ?: it.hashCode()}" },
        ) { anime ->
            val isMovie = anime.format == "MOVIE" || anime.episodes == 1
            val timeStr = remember(anime.airingAt) {
                anime.airingAt?.let { EpisodeReleaseDatePlatform.localTimeAtEpochMs(it * 1000L) }
            }
            val subtitle = remember(anime.nextEpisode, timeStr) {
                val ep = anime.nextEpisode
                when {
                    ep != null && timeStr != null -> "Ep $ep at $timeStr"
                    timeStr != null -> "At $timeStr"
                    ep != null -> "Episode $ep"
                    else -> null
                }
            }

            val preview = remember(anime, subtitle) {
                MetaPreview(
                    id = "ani_${anime.id}",
                    type = if (isMovie) "movie" else "series",
                    name = anime.title,
                    poster = anime.poster,
                    banner = anime.banner,
                    anilistScore = anime.score?.toDouble(),
                    releaseInfo = subtitle,
                )
            }
            HomePosterCard(
                item = preview,
                onClick = { onAnimeClick(preview) },
                onLongClick = onAnimeLongClick?.let { { it(preview) } },
            )
        }
    }
}

@Composable
fun AniChartScreen(
    onAnimeClick: (MetaPreview) -> Unit,
    onAnimeLongClick: ((MetaPreview) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    topChromePadding: Dp = 0.dp,
) {
    CalendarScreen(
        onAnimeClick = onAnimeClick,
        onAnimeLongClick = onAnimeLongClick,
        onBack = onBack,
        modifier = modifier,
        topChromePadding = topChromePadding,
    )
}
