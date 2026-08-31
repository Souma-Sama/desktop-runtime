package com.nuvio.app.features.anilist.anichart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.FluidSlidingSegmentedBar
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioPosterShape
import com.nuvio.app.core.ui.cardWidth
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.formatAnilistScore
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.HomePosterCard
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.rating_anilist
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun AniChartScreen(
    onAnimeClick: (MetaPreview) -> Unit,
    modifier: Modifier = Modifier,
    topChromePadding: Dp = 0.dp,
) {
    val state by AniChartRepository.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val posterCardStyle = rememberPosterCardStyleUiState()

    LaunchedEffect(state.mode, state.selectedSeason, state.selectedYear) {
        if (state.mode == AniChartMode.SEASONAL) {
            AniChartRepository.loadSeasonal(state.selectedSeason, state.selectedYear)
        } else {
            AniChartRepository.loadWeeklySchedule()
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalTopPadding = maxOf(topChromePadding, statusBarPadding + 14.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top Bar
        AniChartHeader(
            mode = state.mode,
            onModeSelected = { AniChartRepository.setMode(it) },
            topPadding = totalTopPadding,
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Controls Bar
        if (state.mode == AniChartMode.SEASONAL) {
            SeasonalControlsBar(
                selectedSeason = state.selectedSeason,
                selectedYear = state.selectedYear,
                selectedFormat = state.selectedFormat,
                onSeasonChange = { s, y -> AniChartRepository.setSeason(s, y) },
                onFormatChange = { AniChartRepository.setFormatFilter(it) },
            )
        } else {
            WeeklyScheduleDayTabs(
                selectedDay = state.selectedDay,
                scheduleItems = state.scheduleItems,
                onDaySelected = { AniChartRepository.setDay(it) },
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area with Adaptive Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {

            if (state.isLoading && (if (state.mode == AniChartMode.SEASONAL) state.seasonalItems.isEmpty() else state.scheduleItems.isEmpty())) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            } else if (state.errorMessage != null && (if (state.mode == AniChartMode.SEASONAL) state.seasonalItems.isEmpty() else state.scheduleItems.isEmpty())) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = state.errorMessage ?: "Unable to load anime chart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                scope.launch {
                                    if (state.mode == AniChartMode.SEASONAL) {
                                        AniChartRepository.loadSeasonal(state.selectedSeason, state.selectedYear, force = true)
                                    } else {
                                        AniChartRepository.loadWeeklySchedule(force = true)
                                    }
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
                if (state.mode == AniChartMode.SEASONAL) {
                    val filteredItems = remember(state.seasonalItems, state.selectedFormat) {
                        when (state.selectedFormat) {
                            AniChartFormatFilter.ALL -> state.seasonalItems
                            AniChartFormatFilter.TV -> state.seasonalItems.filter { it.format == "TV" }
                            AniChartFormatFilter.TV_SHORT -> state.seasonalItems.filter { it.format == "TV_SHORT" }
                            AniChartFormatFilter.MOVIE -> state.seasonalItems.filter { it.format == "MOVIE" }
                            AniChartFormatFilter.OVA_ONA -> state.seasonalItems.filter { it.format in listOf("OVA", "ONA", "SPECIAL") }
                        }
                    }
                    AniChartSeasonalGrid(
                        items = filteredItems,
                        onAnimeClick = onAnimeClick,
                    )
                } else {
                    val dayItems = state.scheduleItems[state.selectedDay].orEmpty()
                    AniChartScheduleGrid(
                        items = dayItems,
                        onAnimeClick = onAnimeClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun AniChartHeader(
    mode: AniChartMode,
    onModeSelected: (AniChartMode) -> Unit,
    topPadding: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = 20.dp, end = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "AniChart",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "AniChart",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Fluid Mode Switcher
        FluidSlidingSegmentedBar(
            items = AniChartMode.entries,
            selectedItem = mode,
            onItemSelected = onModeSelected,
        ) { entry, isSelected ->
            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun SeasonalControlsBar(
    selectedSeason: AniChartSeason,
    selectedYear: Int,
    selectedFormat: AniChartFormatFilter,
    onSeasonChange: (AniChartSeason, Int) -> Unit,
    onFormatChange: (AniChartFormatFilter) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Season / Year Selector
        val seasonScrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .nuvioHorizontalScrollBleed(20.dp)
                .fillMaxWidth()
                .horizontalScroll(seasonScrollState)
                .nuvioDesktopDragScroll(seasonScrollState)
                .padding(horizontal = 20.dp),
        ) {
            FluidSlidingSegmentedBar(
                items = AniChartSeason.entries,
                selectedItem = selectedSeason,
                onItemSelected = { onSeasonChange(it, selectedYear) },
            ) { season, isSelected ->
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${season.label} $selectedYear",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = contentColor,
                    )
                }
            }
        }

        // Format Filter Chips
        val formatScrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .nuvioHorizontalScrollBleed(20.dp)
                .fillMaxWidth()
                .horizontalScroll(formatScrollState)
                .nuvioDesktopDragScroll(formatScrollState)
                .padding(horizontal = 20.dp),
        ) {
            FluidSlidingSegmentedBar(
                items = AniChartFormatFilter.entries,
                selectedItem = selectedFormat,
                onItemSelected = onFormatChange,
                indicatorColor = MaterialTheme.colorScheme.secondary,
            ) { filter, isSelected ->
                val textCol = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        color = textCol,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyScheduleDayTabs(
    selectedDay: AniChartDay,
    scheduleItems: Map<AniChartDay, List<AniChartMedia>>,
    onDaySelected: (AniChartDay) -> Unit,
) {
    val today = remember { AniChartDay.today() }
    val dayScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .nuvioHorizontalScrollBleed(20.dp)
            .fillMaxWidth()
            .horizontalScroll(dayScrollState)
            .nuvioDesktopDragScroll(dayScrollState)
            .padding(horizontal = 20.dp),
    ) {
        FluidSlidingSegmentedBar(
            items = AniChartDay.entries,
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
private fun AniChartSeasonalGrid(
    items: List<AniChartMedia>,
    onAnimeClick: (MetaPreview) -> Unit,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No anime available in this format",
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
    LaunchedEffect(items.firstOrNull()?.id) {
        if (items.isNotEmpty()) {
            runCatching { gridState.scrollToItem(0) }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cardWidth),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { anime ->
            val isMovie = anime.format == "MOVIE" || anime.episodes == 1
            val preview = remember(anime) {
                MetaPreview(
                    id = "ani_${anime.id}",
                    type = if (isMovie) "movie" else "series",
                    name = anime.title,
                    poster = anime.poster,
                    banner = anime.banner,
                    anilistScore = anime.score?.toDouble(),
                    releaseInfo = buildString {
                        if (anime.startDateYear != null) {
                            append(anime.startDateYear)
                        }
                        if (anime.episodes != null && anime.episodes > 0) {
                            if (isNotEmpty()) append("  •  ")
                            append("${anime.episodes} eps")
                        }
                    }.takeIf { it.isNotBlank() },
                )
            }
            HomePosterCard(
                item = preview,
                onClick = { onAnimeClick(preview) },
            )
        }
    }
}

@Composable
private fun AniChartScheduleGrid(
    items: List<AniChartMedia>,
    onAnimeClick: (MetaPreview) -> Unit,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No scheduled anime broadcasts for this day",
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = cardWidth),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = items,
            key = { "${it.id}-${it.airingAt}" },
        ) { anime ->
            val isMovie = anime.format == "MOVIE" || anime.episodes == 1
            val preview = remember(anime) {
                MetaPreview(
                    id = "ani_${anime.id}",
                    type = if (isMovie) "movie" else "series",
                    name = anime.title,
                    poster = anime.poster,
                    banner = anime.banner,
                    anilistScore = anime.score?.toDouble(),
                    releaseInfo = buildString {
                        if (anime.startDateYear != null) {
                            append(anime.startDateYear)
                        }
                        if (anime.episodes != null && anime.episodes > 0) {
                            if (isNotEmpty()) append("  •  ")
                            append("${anime.episodes} eps")
                        }
                    }.takeIf { it.isNotBlank() },
                )
            }
            HomePosterCard(
                item = preview,
                onClick = { onAnimeClick(preview) },
            )
        }
    }
}
