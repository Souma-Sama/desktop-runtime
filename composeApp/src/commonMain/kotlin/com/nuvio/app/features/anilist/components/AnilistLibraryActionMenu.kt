package com.nuvio.app.features.anilist.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.anilist.AnilistLibraryMenuPrefs
import com.nuvio.app.features.anilist.AnilistLibraryMenuPrefsState
import com.nuvio.app.features.anilist.AnilistSortBy

private enum class MenuTab { SORT, OPEN_BY }

@Composable
fun AnilistLibraryActionMenu(
    animeAddons: List<ManagedAddon>,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val prefs by AnilistLibraryMenuPrefs.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        content()

        AnimatedVisibility(
            visible = menuOpen,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .matchParentSize()
                .zIndex(1f),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { menuOpen = false },
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .zIndex(2f),
        ) {
            AnimatedVisibility(
                visible = menuOpen,
                enter = fadeIn(tween(180)) + scaleIn(
                    initialScale = 0.85f,
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                ),
                exit = fadeOut(tween(140)) + scaleOut(
                    targetScale = 0.85f,
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(140),
                ),
            ) {
                AnilistMenuPopup(
                    prefs = prefs,
                    animeAddons = animeAddons,
                    isSyncing = isSyncing,
                    onSyncClick = onSyncClick,
                    onDismiss = { menuOpen = false },
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 340.dp)
                        .shadow(24.dp, RoundedCornerShape(20.dp)),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FloatingActionButton(
                onClick = { menuOpen = !menuOpen },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Library options",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun AnilistMenuPopup(
    prefs: AnilistLibraryMenuPrefsState,
    animeAddons: List<ManagedAddon>,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(MenuTab.SORT) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MenuSegmentedSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (isSyncing) {
                    NuvioLoadingIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Sync AniList",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
            },
            label = "menu_tab_content",
        ) { tab ->
            when (tab) {
                MenuTab.SORT -> SortContent(
                    prefs = prefs,
                    onOptionSelected = { sortBy ->
                        AnilistLibraryMenuPrefs.setSortBy(sortBy)
                        onDismiss()
                    },
                    onToggleDirection = {
                        AnilistLibraryMenuPrefs.setSortAscending(!prefs.sortAscending)
                    },
                )
                MenuTab.OPEN_BY -> OpenByContent(
                    addons = animeAddons,
                    selectedUrl = prefs.openByCatalogUrl,
                    onSelected = { url ->
                        AnilistLibraryMenuPrefs.setOpenByCatalogUrl(url)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuSegmentedSelector(
    selectedTab: MenuTab,
    onTabSelected: (MenuTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = MenuTab.entries
    val selectedIndex = tabs.indexOf(selectedTab)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
    ) {
        val tabWidthDp = maxWidth / tabs.size
        val pillOffsetDp by animateDpAsState(
            targetValue = tabWidthDp * selectedIndex,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
            label = "pill",
        )

        Box(
            modifier = Modifier
                .offset(x = pillOffsetDp)
                .width(tabWidthDp)
                .height(36.dp)
                .shadow(2.dp, RoundedCornerShape(50.dp))
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onTabSelected(tab) },
                ) {
                    Text(
                        text = if (tab == MenuTab.SORT) "Sort" else "Open By",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (tab == selectedTab) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

private data class SortOptionItem(val label: String, val sortBy: AnilistSortBy)

@Composable
private fun SortContent(
    prefs: AnilistLibraryMenuPrefsState,
    onOptionSelected: (AnilistSortBy) -> Unit,
    onToggleDirection: () -> Unit,
) {
    val options = listOf(
        SortOptionItem("Last Updated", AnilistSortBy.LAST_UPDATED),
        SortOptionItem("Score", AnilistSortBy.SCORE),
        SortOptionItem("Title", AnilistSortBy.TITLE),
        SortOptionItem("Release Date", AnilistSortBy.RELEASE_DATE),
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onToggleDirection() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (prefs.sortAscending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (prefs.sortAscending) "Ascending" else "Descending",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        options.forEach { option ->
            SortOptionRow(
                label = option.label,
                isSelected = prefs.sortBy == option.sortBy,
                onClick = { onOptionSelected(option.sortBy) },
            )
        }
    }
}

@Composable
private fun SortOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun OpenByContent(
    addons: List<ManagedAddon>,
    selectedUrl: String?,
    onSelected: (String?) -> Unit,
) {
    Column {
        Text(
            text = "Open By Default Addon",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OpenByRow(
                name = "Auto (Search by Title)",
                logoUrl = null,
                isSelected = selectedUrl == null,
                onClick = { onSelected(null) },
            )

            addons.forEach { addon ->
                val manifest = addon.manifest ?: return@forEach
                OpenByRow(
                    name = addon.displayTitle,
                    logoUrl = manifest.logoUrl,
                    isSelected = selectedUrl == manifest.transportUrl,
                    onClick = { onSelected(manifest.transportUrl) },
                )
            }
        }
    }
}

@Composable
private fun OpenByRow(
    name: String,
    logoUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = name,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
