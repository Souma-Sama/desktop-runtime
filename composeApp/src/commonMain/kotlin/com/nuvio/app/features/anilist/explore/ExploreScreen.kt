package com.nuvio.app.features.anilist.explore

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.nuvio.app.core.ui.LocalNuvioFloatingSidebarPadding
import com.nuvio.app.features.anilist.calendar.CalendarScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.anilist.AnilistAdvancedFilterState
import com.nuvio.app.features.anilist.search.AniHyouSearchFilterSheet
import com.nuvio.app.features.home.MetaPreview

@Composable
fun ExploreScreen(
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    topChromePadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<ExploreCategory?>(null) }
    var customFilter by remember { mutableStateOf<AnilistAdvancedFilterState?>(null) }
    var customSearchQuery by remember { mutableStateOf("") }
    var showSearchFilterSheet by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveTopPadding = maxOf(topChromePadding, statusBarPadding)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isDesktopLayout = maxWidth > 680.dp
        val columnsCount = if (maxWidth > 900.dp) 3 else 2

        if (showCalendar) {
            CalendarScreen(
                onAnimeClick = { preview -> onPosterClick?.invoke(preview) },
                onAnimeLongClick = { preview -> onPosterLongClick?.invoke(preview) },
                onBack = { showCalendar = false },
                topChromePadding = effectiveTopPadding,
            )
        } else if (selectedCategory != null || customFilter != null) {
            ExploreMediaListView(
                category = selectedCategory,
                customFilter = customFilter,
                searchQuery = customSearchQuery,
                onBack = {
                    selectedCategory = null
                    customFilter = null
                    customSearchQuery = ""
                },
                topChromePadding = effectiveTopPadding,
                onPosterClick = onPosterClick,
                onPosterLongClick = onPosterLongClick,
            )
        } else {
            val floatingSidebarPadding = LocalNuvioFloatingSidebarPadding.current
            val startPadding = if (floatingSidebarPadding > 0.dp) floatingSidebarPadding else 16.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = startPadding,
                        top = effectiveTopPadding + 14.dp,
                        end = 16.dp,
                        bottom = 100.dp,
                    ),
            ) {
                // Top Search Bar (AniHyou style)
                Surface(
                    color = Color(0xFF232428),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable { showSearchFilterSheet = true },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Anime, Manga, and More",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Anime Section ---
                Text(
                    text = "Anime",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))

                val animeCategories = listOf(
                    ExploreCategory.TOP_100_ANIME,
                    ExploreCategory.TOP_POPULAR_ANIME,
                    ExploreCategory.UPCOMING_ANIME,
                    ExploreCategory.AIRING_ANIME,
                    ExploreCategory.SPRING_ANIME,
                    ExploreCategory.SUMMER_ANIME,
                    ExploreCategory.FALL_ANIME,
                    ExploreCategory.WINTER_ANIME,
                    ExploreCategory.TOP_MOVIES_ANIME,
                    ExploreCategory.CALENDAR,
                )

                animeCategories.chunked(columnsCount).forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        group.forEach { cat ->
                            ExploreCategoryCard(
                                category = cat,
                                onClick = {
                                    if (cat == ExploreCategory.CALENDAR) {
                                        showCalendar = true
                                    } else {
                                        selectedCategory = cat
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columnsCount - group.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- Manga Section ---
                Text(
                    text = "Manga",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))

                val mangaCategories = listOf(
                    ExploreCategory.TOP_100_MANGA,
                    ExploreCategory.TOP_POPULAR_MANGA,
                    ExploreCategory.UPCOMING_MANGA,
                    ExploreCategory.PUBLISHING_MANGA,
                )

                mangaCategories.chunked(columnsCount).forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        group.forEach { cat ->
                            ExploreCategoryCard(
                                category = cat,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columnsCount - group.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (showSearchFilterSheet) {
            AniHyouSearchFilterSheet(
                initialFilter = remember { AnilistAdvancedFilterState() },
                initialQuery = customSearchQuery,
                isDesktop = isDesktopLayout,
                onDismiss = { showSearchFilterSheet = false },
                onApply = { newFilter, newQuery ->
                    showSearchFilterSheet = false
                    selectedCategory = null
                    customFilter = newFilter
                    customSearchQuery = newQuery
                },
            )
        }
    }
}

@Composable
private fun ExploreCategoryCard(
    category: ExploreCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color(0xFF232428),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
