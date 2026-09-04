package com.nuvio.app.features.anilist.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.nuvio.app.features.anilist.AnilistSortOption
import com.nuvio.app.features.anilist.calendar.AnimeSeason

private val SheetBgColor = Color(0xFF1B1C20)
private val ChipBgColor = Color(0xFF232428)
private val ChipSelectedBgColor = Color(0xFF383A42)
private val ButtonBorderColor = Color(0xFF3E4046)

private val seasonSortEntries = listOf(
    Pair("Popularity", AnilistSortOption.POPULARITY),
    Pair("Score", AnilistSortOption.SCORE),
    Pair("Start date", AnilistSortOption.NEWEST),
    Pair("End date", AnilistSortOption.END_DATE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonChartFilterSheet(
    initialSeason: AnimeSeason,
    initialYear: Int,
    initialSort: AnilistSortOption,
    isDesktop: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (AnimeSeason, Int, AnilistSortOption) -> Unit,
) {
    var selectedSeason by remember { mutableStateOf(initialSeason) }
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedSort by remember { mutableStateOf(initialSort) }

    if (isDesktop) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 460.dp, max = 520.dp)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = SheetBgColor,
                border = BorderStroke(1.dp, ButtonBorderColor),
            ) {
                SeasonChartFilterContent(
                    selectedSeason = selectedSeason,
                    selectedYear = selectedYear,
                    selectedSort = selectedSort,
                    onSeasonChange = { selectedSeason = it },
                    onYearChange = { selectedYear = it },
                    onSortChange = { selectedSort = it },
                    onCancel = onDismiss,
                    onApply = {
                        onApply(selectedSeason, selectedYear, selectedSort)
                        onDismiss()
                    },
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = SheetBgColor,
            dragHandle = null,
        ) {
            SeasonChartFilterContent(
                selectedSeason = selectedSeason,
                selectedYear = selectedYear,
                selectedSort = selectedSort,
                onSeasonChange = { selectedSeason = it },
                onYearChange = { selectedYear = it },
                onSortChange = { selectedSort = it },
                onCancel = onDismiss,
                onApply = {
                    onApply(selectedSeason, selectedYear, selectedSort)
                    onDismiss()
                },
                modifier = Modifier.padding(bottom = maxOf(24.dp, navBarPadding + 16.dp)),
            )
        }
    }
}

@Composable
private fun SeasonChartFilterContent(
    selectedSeason: AnimeSeason,
    selectedYear: Int,
    selectedSort: AnilistSortOption,
    onSeasonChange: (AnimeSeason) -> Unit,
    onYearChange: (Int) -> Unit,
    onSortChange: (AnilistSortOption) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentYear = remember { AnimeSeason.currentYear() }
    val years = remember { ((currentYear + 1) downTo 1970).toList() }
    val yearListState = rememberLazyListState()

    LaunchedEffect(selectedYear) {
        val idx = years.indexOf(selectedYear)
        if (idx >= 0) {
            yearListState.scrollToItem(maxOf(0, idx - 2))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- 1. TOP HEADER (Cancel / Drag Handle / Apply) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4E5057)),
            )

            Button(
                onClick = onApply,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1B1C20),
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "Apply",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. SEASON BUTTONS ROW ---
        val seasons = listOf(
            Pair(AnimeSeason.WINTER, ExploreIcons.AcUnit),
            Pair(AnimeSeason.SPRING, ExploreIcons.LocalFlorist),
            Pair(AnimeSeason.SUMMER, ExploreIcons.Sunny),
            Pair(AnimeSeason.FALL, ExploreIcons.Rainy),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            seasons.forEach { (season, icon) ->
                val isSelected = season == selectedSeason
                Surface(
                    onClick = { onSeasonChange(season) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color.White else Color(0xFF28292E),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = season.label,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) Color.Black else Color(0xFFC4C6D0),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // --- 3. YEAR SELECTOR ROW ---
        LazyRow(
            state = yearListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(years, key = { it }) { year ->
                val isSelected = year == selectedYear
                Surface(
                    onClick = { onYearChange(year) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ChipSelectedBgColor else ChipBgColor,
                    border = if (isSelected) BorderStroke(1.dp, Color(0xFF5E6068)) else null,
                ) {
                    Text(
                        text = year.toString(),
                        color = if (isSelected) Color.White else Color(0xFF9E9FA6),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. SORT DROPDOWN SELECTOR ---
        var sortMenuExpanded by remember { mutableStateOf(false) }
        val currentSortLabel = seasonSortEntries.firstOrNull { it.second == selectedSort }?.first ?: "Popularity"

        Box {
            Surface(
                onClick = { sortMenuExpanded = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF28292E),
                border = BorderStroke(1.dp, ButtonBorderColor),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Sort",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFC4C6D0),
                    )
                    Text(
                        text = currentSortLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFC4C6D0),
                    )
                }
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                modifier = Modifier
                    .background(Color(0xFF25262B))
                    .widthIn(min = 160.dp),
            ) {
                seasonSortEntries.forEach { (label, sortOption) ->
                    val isSelected = sortOption == selectedSort
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFFC4C6D0),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                            )
                        },
                        modifier = Modifier.background(
                            if (isSelected) Color(0xFF383A42) else Color.Transparent
                        ),
                        onClick = {
                            onSortChange(sortOption)
                            sortMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
}
