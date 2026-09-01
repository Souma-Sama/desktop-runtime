package com.nuvio.app.features.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.PosterGridRow
import com.nuvio.app.features.home.components.PosterGridSkeletonRow
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.discoverContent(
    state: DiscoverUiState,
    columns: Int,
    networkCondition: NetworkCondition,
    onTypeSelected: (String) -> Unit,
    onCatalogSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onSortSelected: (String?) -> Unit = {},
    onOpenAdvancedFilter: () -> Unit = {},
    activeAdvancedFilterCount: Int = 0,
    onRetry: (() -> Unit)? = null,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    item {
        DiscoverSectionHeader(modifier = Modifier.padding(horizontal = 16.dp))
    }
    item {
        DiscoverFilterRow(
            state = state,
            modifier = Modifier.padding(horizontal = 16.dp),
            onTypeSelected = onTypeSelected,
            onCatalogSelected = onCatalogSelected,
            onGenreSelected = onGenreSelected,
            onSortSelected = onSortSelected,
            onOpenAdvancedFilter = onOpenAdvancedFilter,
            activeAdvancedFilterCount = activeAdvancedFilterCount,
        )
    }
    state.selectedCatalog?.let { selectedCatalog ->
        item {
            Text(
                text = stringResource(
                    Res.string.discover_catalog_context,
                    selectedCatalog.addonName,
                    selectedCatalog.type.displayTypeLabel(),
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    when {
        state.isLoading && state.items.isEmpty() -> {
            items(2) {
                PosterGridSkeletonRow(
                    columns = columns,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        state.items.isEmpty() -> {
            item {
                DiscoverEmptyStateCard(
                    reason = state.emptyStateReason,
                    errorMessage = state.errorMessage,
                    networkCondition = networkCondition,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        else -> {
            items(state.items.chunked(columns)) { rowItems ->
                PosterGridRow(
                    items = rowItems,
                    columns = columns,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    watchedKeys = watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onPosterClick = onPosterClick,
                    onPosterLongClick = onPosterLongClick,
                )
            }
            if (state.isLoading) {
                item {
                    CatalogLoadingFooter(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverSectionHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.compose_search_discover_title),
        modifier = modifier,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun DiscoverFilterRow(
    state: DiscoverUiState,
    onTypeSelected: (String) -> Unit,
    onCatalogSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onSortSelected: (String?) -> Unit,
    onOpenAdvancedFilter: () -> Unit,
    activeAdvancedFilterCount: Int,
    modifier: Modifier = Modifier,
) {
    val anilistPrefs = com.nuvio.app.features.anilist.AnilistPreferencesRepository.snapshot()
    val isAnime = state.selectedType.equals("anime", ignoreCase = true) || state.selectedCatalogKey?.contains("anilist", ignoreCase = true) == true
    val showAdvancedFilter = anilistPrefs.enabled && anilistPrefs.enableAdvancedFilters && isAnime

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NuvioDropdownChip(
            title = stringResource(Res.string.discover_select_type),
            label = state.selectedType?.displayTypeLabel() ?: stringResource(Res.string.discover_type),
            selectedKey = state.selectedType,
            options = state.typeOptions.map { NuvioDropdownOption(key = it, label = it.displayTypeLabel()) },
            enabled = state.typeOptions.isNotEmpty(),
            onSelected = { onTypeSelected(it.key) },
        )
        NuvioDropdownChip(
            title = stringResource(Res.string.discover_select_catalog),
            label = state.selectedCatalog?.catalogName ?: stringResource(Res.string.discover_catalog),
            selectedKey = state.selectedCatalogKey,
            options = state.catalogOptions.map { option -> NuvioDropdownOption(key = option.key, label = option.catalogName) },
            enabled = state.catalogOptions.isNotEmpty(),
            onSelected = { onCatalogSelected(it.key) },
        )

        val selectedCatalog = state.selectedCatalog
        val genreOptions = buildList {
            if (selectedCatalog?.genreRequired != true) {
                add(NuvioDropdownOption(key = "", label = stringResource(Res.string.discover_all_genres)))
            }
            addAll(state.genreOptions.map { genre -> NuvioDropdownOption(key = genre, label = genre) })
        }
        NuvioDropdownChip(
            title = stringResource(Res.string.discover_select_genre),
            label = state.selectedGenre ?: stringResource(Res.string.discover_all_genres),
            selectedKey = state.selectedGenre ?: "",
            options = genreOptions,
            enabled = genreOptions.size > 1 || selectedCatalog?.genreRequired == true,
            onSelected = { option ->
                onGenreSelected(option.key.ifBlank { null })
            },
        )

        if (state.sortOptions.isNotEmpty()) {
            val sortOptions = state.sortOptions.map { sort -> NuvioDropdownOption(key = sort, label = sort) }
            val currentSortLabel = state.selectedSort ?: sortOptions.firstOrNull()?.label ?: "Popularity"
            NuvioDropdownChip(
                title = "Sort",
                label = currentSortLabel,
                selectedKey = state.selectedSort ?: sortOptions.firstOrNull()?.key,
                options = sortOptions,
                enabled = true,
                onSelected = { option ->
                    onSortSelected(option.key)
                },
            )
        }

        if (showAdvancedFilter) {
            val hasActive = activeAdvancedFilterCount > 0
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenAdvancedFilter() },
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
                        contentDescription = "Advanced Filters",
                        tint = if (hasActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = if (hasActive) "Filters ($activeAdvancedFilterCount)" else "Filters",
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
}

@Composable
private fun CatalogLoadingFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DiscoverEmptyStateCard(
    reason: DiscoverEmptyStateReason?,
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable) {
        NuvioNetworkOfflineCard(
            condition = networkCondition,
            modifier = modifier,
            onRetry = onRetry,
        )
        return
    }

    val title: String
    val message: String

    when (reason) {
        DiscoverEmptyStateReason.NoActiveAddons -> {
            title = stringResource(Res.string.compose_search_empty_no_active_addons_title)
            message = stringResource(Res.string.discover_empty_no_active_addons_message)
        }

        DiscoverEmptyStateReason.NoDiscoverCatalogs -> {
            title = stringResource(Res.string.discover_empty_no_catalogs_title)
            message = stringResource(Res.string.discover_empty_no_catalogs_message)
        }

        DiscoverEmptyStateReason.RequestFailed -> {
            title = stringResource(Res.string.discover_empty_load_failed_title)
            message = errorMessage ?: stringResource(Res.string.discover_empty_load_failed_message)
        }

        DiscoverEmptyStateReason.NoResults, null -> {
            title = stringResource(Res.string.discover_empty_no_results_title)
            message = stringResource(Res.string.discover_empty_no_results_message)
        }
    }

    HomeEmptyStateCard(
        modifier = modifier,
        title = title,
        message = message,
    )
}

@Composable
private fun String.displayTypeLabel(): String =
    when (lowercase()) {
        "movie" -> stringResource(Res.string.media_movies)
        "series" -> stringResource(Res.string.media_series)
        "anime" -> stringResource(Res.string.media_anime)
        "channel" -> stringResource(Res.string.media_channels)
        "tv" -> stringResource(Res.string.media_tv)
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
