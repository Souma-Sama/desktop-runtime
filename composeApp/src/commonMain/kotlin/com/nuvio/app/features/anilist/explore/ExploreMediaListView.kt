package com.nuvio.app.features.anilist.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import com.nuvio.app.core.ui.LocalNuvioFloatingSidebarPadding
import com.nuvio.app.features.anilist.calendar.AnimeSeason
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.anilist.AnilistAdvancedFilterState
import com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.PosterGridRow
import com.nuvio.app.features.home.components.PosterGridSkeletonRow
import com.nuvio.app.features.home.components.posterGridColumnCountForWidth
import com.nuvio.app.features.watched.WatchedRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
internal fun ExploreMediaListView(
    category: ExploreCategory? = null,
    customFilter: AnilistAdvancedFilterState? = null,
    searchQuery: String = "",
    titleOverride: String? = null,
    onBack: () -> Unit,
    topChromePadding: Dp = 0.dp,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var activeFilter by remember(category, customFilter) {
        mutableStateOf(customFilter ?: category?.toFilterState() ?: AnilistAdvancedFilterState())
    }

    val isSeasonalCategory = category in listOf(
        ExploreCategory.SPRING_ANIME,
        ExploreCategory.SUMMER_ANIME,
        ExploreCategory.FALL_ANIME,
        ExploreCategory.WINTER_ANIME,
    ) || (activeFilter.season != null && activeFilter.seasonYear != null)

    var currentSeason by remember(category, activeFilter.season) {
        val s = when (category) {
            ExploreCategory.SPRING_ANIME -> AnimeSeason.SPRING
            ExploreCategory.SUMMER_ANIME -> AnimeSeason.SUMMER
            ExploreCategory.FALL_ANIME -> AnimeSeason.FALL
            ExploreCategory.WINTER_ANIME -> AnimeSeason.WINTER
            else -> AnimeSeason.entries.firstOrNull { it.apiName.equals(activeFilter.season, ignoreCase = true) } ?: AnimeSeason.current()
        }
        mutableStateOf(s)
    }
    var currentYear by remember(category, activeFilter.seasonYear) {
        mutableIntStateOf(activeFilter.seasonYear ?: AnimeSeason.currentYear())
    }
    var currentSort by remember(category, activeFilter.sort) {
        mutableStateOf(activeFilter.sort)
    }

    var activeTitle by remember(category, customFilter, titleOverride, isSeasonalCategory, currentSeason, currentYear) {
        val initial = if (isSeasonalCategory) {
            "${currentSeason.label} $currentYear"
        } else {
            titleOverride ?: category?.fullDisplayTitle ?: if (searchQuery.isNotBlank()) "Search: \"$searchQuery\"" else "Explore Chart"
        }
        mutableStateOf(initial)
    }

    var showSeasonFilterSheet by remember { mutableStateOf(false) }

    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val watchedKeys = watchedUiState.watchedKeys

    val items = remember { mutableStateListOf<MetaPreview>() }
    var page by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun loadPage(targetPage: Int, append: Boolean = false) {
        coroutineScope.launch {
            if (targetPage == 1) {
                isLoading = true
                errorMessage = null
            } else {
                isLoadingMore = true
            }

            try {
                val catalogPage = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                    AnilistCatalogRepository.fetchAdvancedFilterPage(
                        filter = activeFilter,
                        searchQuery = searchQuery,
                        page = targetPage,
                        perPage = 30,
                    )
                } ?: throw IllegalStateException("Request timed out. Please check your network connection.")

                if (append) {
                    val existingIds = items.map { it.id }.toSet()
                    val newItems = catalogPage.items.filter { it.id !in existingIds }
                    items.addAll(newItems)
                } else {
                    items.clear()
                    items.addAll(catalogPage.items)
                }
                hasMore = catalogPage.items.size >= 25
                page = targetPage
            } catch (e: Exception) {
                if (targetPage == 1) {
                    errorMessage = e.message ?: "Failed to load chart"
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(category, customFilter, searchQuery, activeFilter) {
        loadPage(1, append = false)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isDesktopLayout = maxWidth > 680.dp
        val columns = posterGridColumnCountForWidth(maxWidth)
        val rows = remember(items.size, columns) { items.chunked(columns) }

        LaunchedEffect(listState, rows.size, hasMore, isLoading, isLoadingMore) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { lastVisibleIndex ->
                    if (hasMore && !isLoading && !isLoadingMore && rows.isNotEmpty() && lastVisibleIndex >= rows.size - 3) {
                        loadPage(page + 1, append = true)
                    }
                }
        }

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val effectiveTopPadding = maxOf(topChromePadding, statusBarPadding + 4.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            if (effectiveTopPadding > 0.dp) {
                Spacer(modifier = Modifier.height(effectiveTopPadding))
            }

            // Top bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
                val topBarStartPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 12.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = topBarStartPadding, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    val activeIcon: ImageVector = when {
                        isSeasonalCategory -> when (currentSeason) {
                            AnimeSeason.WINTER -> ExploreIcons.AcUnit
                            AnimeSeason.SPRING -> ExploreIcons.LocalFlorist
                            AnimeSeason.SUMMER -> ExploreIcons.Sunny
                            AnimeSeason.FALL -> ExploreIcons.Rainy
                        }
                        category != null -> category.icon
                        else -> Icons.Default.Search
                    }

                    Icon(
                        imageVector = activeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (items.isNotEmpty()) {
                            Text(
                                text = "${items.size} titles loaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Content List
            if (isLoading && items.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    items(6) {
                        PosterGridSkeletonRow(
                            columns = columns,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            } else if (errorMessage != null && items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "Error Loading Chart",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { loadPage(1, append = false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No media found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = if (isDesktopLayout) 80.dp else 116.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = rows,
                        key = { row -> row.firstOrNull()?.id ?: row.hashCode() },
                    ) { rowItems ->
                        val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
                        val rowStartPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 16.dp
                        PosterGridRow(
                            items = rowItems,
                            columns = columns,
                            modifier = Modifier.padding(start = rowStartPadding, end = 16.dp),
                            watchedKeys = watchedKeys,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            onPosterClick = onPosterClick,
                            onPosterLongClick = onPosterLongClick,
                        )
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isSeasonalCategory) {
            val fabBottomPadding = if (isDesktopLayout) 28.dp else 104.dp
            FloatingActionButton(
                onClick = { showSeasonFilterSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = fabBottomPadding),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Season Filter",
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (showSeasonFilterSheet) {
            SeasonChartFilterSheet(
                initialSeason = currentSeason,
                initialYear = currentYear,
                initialSort = currentSort,
                isDesktop = isDesktopLayout,
                onDismiss = { showSeasonFilterSheet = false },
                onApply = { newSeason, newYear, newSort ->
                    currentSeason = newSeason
                    currentYear = newYear
                    currentSort = newSort
                    activeTitle = "${newSeason.label} $newYear"
                    activeFilter = activeFilter.copy(
                        season = newSeason.apiName,
                        seasonYear = newYear,
                        sort = newSort,
                    )
                },
            )
        }
    }
}
