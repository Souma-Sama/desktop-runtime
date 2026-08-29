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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.launch

private fun aniChartColumnCountForWidth(screenWidth: Dp): Int =
    when {
        screenWidth >= 1400.dp -> 7
        screenWidth >= 1200.dp -> 6
        screenWidth >= 1000.dp -> 5
        screenWidth >= 840.dp -> 4
        else -> 3
    }

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
            onRefresh = {
                scope.launch {
                    if (state.mode == AniChartMode.SEASONAL) {
                        AniChartRepository.loadSeasonal(state.selectedSeason, state.selectedYear, force = true)
                    } else {
                        AniChartRepository.loadWeeklySchedule(force = true)
                    }
                }
            },
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

        // Content Area with Screen-Width-Aware Columns
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val columns = remember(maxWidth) { aniChartColumnCountForWidth(maxWidth) }

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
                        columns = columns,
                        cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                        onAnimeClick = onAnimeClick,
                    )
                } else {
                    val dayItems = state.scheduleItems[state.selectedDay].orEmpty()
                    AniChartScheduleGrid(
                        items = dayItems,
                        columns = columns,
                        cornerRadiusDp = posterCardStyle.cornerRadiusDp,
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
    onRefresh: () -> Unit,
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Mode Switcher Glass Pill
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    AniChartMode.entries.forEach { entry ->
                        val isSelected = entry == mode
                        val bg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = tween(180),
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(bg)
                                .clickable { onModeSelected(entry) }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
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

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Season / Year Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = {
                    val prevSeasonIndex = selectedSeason.ordinal - 1
                    if (prevSeasonIndex < 0) {
                        onSeasonChange(AniChartSeason.FALL, selectedYear - 1)
                    } else {
                        onSeasonChange(AniChartSeason.entries[prevSeasonIndex], selectedYear)
                    }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Season", tint = MaterialTheme.colorScheme.onSurface)
            }

            val seasonList = AniChartSeason.entries
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                seasonList.forEach { season ->
                    val isSelected = season == selectedSeason
                    val containerColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                    val contentColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(containerColor)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSeasonChange(season, selectedYear) }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${season.label} $selectedYear",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            color = contentColor,
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val nextSeasonIndex = selectedSeason.ordinal + 1
                    if (nextSeasonIndex >= AniChartSeason.entries.size) {
                        onSeasonChange(AniChartSeason.WINTER, selectedYear + 1)
                    } else {
                        onSeasonChange(AniChartSeason.entries[nextSeasonIndex], selectedYear)
                    }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Season", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Format Filter Chips Row
        val chipRowState = rememberLazyListState()
        LazyRow(
            state = chipRowState,
            modifier = Modifier
                .fillMaxWidth()
                .nuvioDesktopDragScroll(chipRowState),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AniChartFormatFilter.entries) { filter ->
                val isSelected = filter == selectedFormat
                val bg by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
                val textCol by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable { onFormatChange(filter) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
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
    val rowState = rememberLazyListState()

    LazyRow(
        state = rowState,
        modifier = Modifier
            .fillMaxWidth()
            .nuvioDesktopDragScroll(rowState),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(AniChartDay.entries) { day ->
            val isSelected = day == selectedDay
            val isToday = day == today
            val count = scheduleItems[day]?.size ?: 0

            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onDaySelected(day) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${day.label}${if (count > 0) " ($count)" else ""}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                        color = textColor,
                    )
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
}

@Composable
private fun AniChartSeasonalGrid(
    items: List<AniChartMedia>,
    columns: Int,
    cornerRadiusDp: Int,
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

    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { anime ->
            AniChartCard(
                media = anime,
                cornerRadiusDp = cornerRadiusDp,
                onClick = {
                    val isMovie = anime.format == "MOVIE" || anime.episodes == 1
                    onAnimeClick(
                        MetaPreview(
                            id = "ani_${anime.id}",
                            type = if (isMovie) "movie" else "series",
                            name = anime.title,
                            poster = anime.poster,
                            banner = anime.banner,
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun AniChartScheduleGrid(
    items: List<AniChartMedia>,
    columns: Int,
    cornerRadiusDp: Int,
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

    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(
            items = items,
            key = { "${it.id}-${it.airingAt}" },
        ) { anime ->
            AniChartCard(
                media = anime,
                cornerRadiusDp = cornerRadiusDp,
                showAiringTime = true,
                onClick = {
                    val isMovie = anime.format == "MOVIE" || anime.episodes == 1
                    onAnimeClick(
                        MetaPreview(
                            id = "ani_${anime.id}",
                            type = if (isMovie) "movie" else "series",
                            name = anime.title,
                            poster = anime.poster,
                            banner = anime.banner,
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun AniChartCard(
    media: AniChartMedia,
    cornerRadiusDp: Int,
    modifier: Modifier = Modifier,
    showAiringTime: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "anichart_card_scale",
    )
    val cardShape = RoundedCornerShape(cornerRadiusDp.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                .border(
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                    ),
                    shape = cardShape,
                )
                .nuvioCardDepth(
                    shape = cardShape,
                    surface = NuvioCardDepthSurface.Posters,
                ),
        ) {
            if (!media.poster.isNullOrBlank()) {
                AsyncImage(
                    model = media.poster,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Multi-stop overlay gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f),
                            ),
                            startY = 0f,
                        )
                    )
            )

            // Top Left: Airing countdown badge or Continuing badge
            val timeUntil = media.timeUntilAiring
            val nextEp = media.nextEpisode
            if (nextEp != null && timeUntil != null && timeUntil > 0) {
                val days = timeUntil / 86400
                val hours = (timeUntil % 86400) / 3600
                val mins = (timeUntil % 3600) / 60
                val timeStr = when {
                    days > 0 -> "${days}d ${hours}h"
                    hours > 0 -> "${hours}h ${mins}m"
                    else -> "${mins}m"
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.92f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "Ep $nextEp in $timeStr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            } else if (media.isContinuing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.90f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "Continuing",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSecondary,
                        maxLines = 1,
                    )
                }
            }

            // Top Right: Score badge
            if (media.score != null && media.score > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Score",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = media.score.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = Color.White,
                        )
                    }
                }
            }

            // Bottom Info: Studio and format
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 7.dp, vertical = 7.dp),
            ) {
                if (!media.studio.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = media.studio,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                val formatText = listOfNotNull(
                    media.format?.takeIf { it.isNotBlank() },
                    media.episodes?.takeIf { it > 0 }?.let { "$it eps" },
                ).joinToString(" • ")
                if (formatText.isNotBlank()) {
                    Text(
                        text = formatText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = media.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 15.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
