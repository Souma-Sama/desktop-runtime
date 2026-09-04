package com.nuvio.app.features.anilist.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import com.nuvio.app.core.ui.nuvioHorizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioDesktopVerticalScrollbar
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.community.AnilistContentBlockItem
import com.nuvio.app.features.anilist.community.parseAnilistRichContent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AniHyouCyanAccent = Color(0xFF40C4FF)
private val AniHyouDarkBackground = Color(0xFF0C0D10)
private val AniHyouCardSurface = Color(0xFF16181F)
private val AniHyouChipInactiveBg = Color(0xFF1F222B)
private val AniHyouChipActiveBg = Color(0xFF323642)
private val AniHyouChipBorder = Color(0xFF2C303D)
private val AniHyouSubtleText = Color(0xFF8E92A0)

private enum class AniHyouProfileTab(val label: String, val icon: ImageVector) {
    INFO("Info", Icons.Rounded.Info),
    ACTIVITY("Activity", Icons.AutoMirrored.Rounded.Chat),
    STATS("Stats", Icons.Rounded.BarChart),
    FAVORITES("Favorites", Icons.Rounded.Star),
    SOCIAL("Social", Icons.Rounded.People),
}

private enum class AniHyouStatsCategory(val label: String) {
    OVERVIEW("Overview"),
    GENRES("Genres"),
    TAGS("Tags"),
    STAFF("Staff"),
    VOICE_ACTORS("Voice Actors"),
    STUDIOS("Studios"),
}

private enum class AniHyouMediumType(val label: String) {
    ANIME("Anime"),
    MANGA("Manga"),
}

private enum class AniHyouStatSortMetric(val label: String) {
    TITLES("Titles"),
    TIME("Time"),
    SCORE("Score"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnilistUserProfileSheet(
    userId: Int? = null,
    username: String? = null,
    onDismiss: () -> Unit,
    onAnimeClick: ((Int) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<AnilistFullUserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(AniHyouProfileTab.STATS) }
    var isFollowing by remember { mutableStateOf(false) }

    val currentUserId = AnilistAuthRepository.currentUser.value?.id
    val isSelf = currentUserId != null && currentUserId == userId
    val isLoggedIn = AnilistAuthRepository.isAuthenticated.value

    LaunchedEffect(userId, username) {
        isLoading = true
        errorMessage = null
        val result = AnilistProfileRepository.fetchUserProfile(userId = userId, username = username)
        result.onSuccess {
            profile = it
            isFollowing = it.isFollowing
            isLoading = false
        }.onFailure {
            errorMessage = it.message ?: "Failed to load profile"
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AniHyouDarkBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(720.dp),
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AniHyouCyanAccent)
                }
            } else if (errorMessage != null || profile == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = errorMessage ?: "Profile not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AniHyouCyanAccent),
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val user = profile!!
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(14.dp)) }

                    item {
                        AniHyouHeaderRow(
                            user = user,
                            isSelf = isSelf,
                            isLoggedIn = isLoggedIn,
                            isFollowing = isFollowing,
                            onToggleFollow = {
                                scope.launch {
                                    val newFollow = !isFollowing
                                    isFollowing = newFollow
                                    AnilistProfileRepository.toggleFollow(user.id)
                                }
                            },
                        )
                    }

                    item {
                        AniHyouPillNavigationBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                        )
                    }

                    when (selectedTab) {
                        AniHyouProfileTab.INFO -> item { AniHyouInfoTabContent(user = user) }
                        AniHyouProfileTab.ACTIVITY -> item { AniHyouActivityTabContent(userId = user.id, onAnimeClick = onAnimeClick) }
                        AniHyouProfileTab.STATS -> item { AniHyouStatsTabContent(userId = user.id) }
                        AniHyouProfileTab.FAVORITES -> item { AniHyouFavoritesTabContent(user = user, onAnimeClick = onAnimeClick) }
                        AniHyouProfileTab.SOCIAL -> item { AniHyouSocialTabContent(userId = user.id) }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }

                NuvioDesktopVerticalScrollbar(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AniHyouHeaderRow(
    user: AnilistFullUserProfile,
    isSelf: Boolean,
    isLoggedIn: Boolean,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(AniHyouCardSurface)
                    .border(2.dp, AniHyouChipBorder, CircleShape),
            ) {
                val avatarUrl = user.avatarLarge ?: user.avatarMedium
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AniHyouChipActiveBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
            }

            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = Color.White,
                )
                if (user.donatorTier > 0 && !user.donatorBadge.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFE50914).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = user.donatorBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = Color(0xFFFF5252),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isLoggedIn && !isSelf) {
                OutlinedButton(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isFollowing) AniHyouChipBorder else AniHyouCyanAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isFollowing) AniHyouChipActiveBg else AniHyouCyanAccent.copy(alpha = 0.15f),
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = if (isFollowing) Color.White else AniHyouCyanAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isFollowing) Color.White else AniHyouCyanAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun AniHyouPillNavigationBar(
    selectedTab: AniHyouProfileTab,
    onTabSelected: (AniHyouProfileTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AniHyouCardSurface,
        border = BorderStroke(1.dp, AniHyouChipBorder.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AniHyouProfileTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    animationSpec = tween(180),
                )
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else AniHyouSubtleText,
                    animationSpec = tween(180),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(containerColor)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AniHyouFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) AniHyouChipActiveBg else AniHyouChipInactiveBg
    val border = if (selected) Color(0xFF4A4E5C) else AniHyouChipBorder
    val textColor = if (selected) Color.White else AniHyouSubtleText

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = textColor,
            )
        }
    }
}

@Composable
private fun AniHyouStatsTabContent(
    userId: Int,
) {
    var statsState by remember { mutableStateOf<AnilistUserStatistics?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedCategory by remember { mutableStateOf(AniHyouStatsCategory.OVERVIEW) }
    var selectedMedium by remember { mutableStateOf(AniHyouMediumType.ANIME) }
    var scoreSortMetric by remember { mutableStateOf(AniHyouStatSortMetric.TITLES) }
    var lengthSortMetric by remember { mutableStateOf(AniHyouStatSortMetric.TITLES) }
    var releaseYearSortMetric by remember { mutableStateOf(AniHyouStatSortMetric.TITLES) }
    var watchYearSortMetric by remember { mutableStateOf(AniHyouStatSortMetric.TITLES) }
    var generalSortMetric by remember { mutableStateOf(AniHyouStatSortMetric.TITLES) }

    LaunchedEffect(userId) {
        isLoading = true
        errorMessage = null
        val result = AnilistProfileRepository.fetchUserStatistics(userId)
        result.fold(
            onSuccess = { stats ->
                statsState = stats
                isLoading = false
            },
            onFailure = { err ->
                errorMessage = err.message ?: "Failed to load statistics"
                isLoading = false
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val categoryScrollState = rememberScrollState()
        // Horizontally scrolling category chips: Overview, Genres, Tags, Staff, Voice Actors, Studios
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .nuvioHorizontalScroll(categoryScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AniHyouStatsCategory.entries.forEach { cat ->
                AniHyouFilterChip(
                    label = cat.label,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                )
            }
        }

        // Medium selection chips: Anime, Manga
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AniHyouMediumType.entries.forEach { med ->
                AniHyouFilterChip(
                    label = med.label,
                    selected = selectedMedium == med,
                    onClick = { selectedMedium = med },
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AniHyouCyanAccent, strokeWidth = 2.dp)
            }
        } else if (errorMessage != null || statsState == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = errorMessage ?: "No statistics available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AniHyouSubtleText,
                )
            }
        } else {
            val stats = statsState!!
            val mediumStats = if (selectedMedium == AniHyouMediumType.ANIME) stats.anime else stats.manga
            val isAnime = selectedMedium == AniHyouMediumType.ANIME

            when (selectedCategory) {
                AniHyouStatsCategory.OVERVIEW -> {
                    AniHyouMetricGrid(stats = mediumStats, isAnime = isAnime)

                    Spacer(modifier = Modifier.height(8.dp))
                    AniHyouScoreSection(
                        scores = mediumStats.scores,
                        sortMetric = scoreSortMetric,
                        onSortChange = { scoreSortMetric = it },
                        isAnime = isAnime,
                    )

                    if (mediumStats.lengths.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AniHyouLengthSection(
                            lengths = mediumStats.lengths,
                            sortMetric = lengthSortMetric,
                            onSortChange = { lengthSortMetric = it },
                            isAnime = isAnime,
                        )
                    }

                    if (mediumStats.statuses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AniHyouStatusSection(statuses = mediumStats.statuses, isAnime = isAnime)
                    }

                    if (mediumStats.formats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AniHyouFormatSection(formats = mediumStats.formats)
                    }

                    if (mediumStats.countries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AniHyouCountrySection(countries = mediumStats.countries)
                    }

                    if (mediumStats.releaseYears.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AniHyouReleaseYearSection(
                            releaseYears = mediumStats.releaseYears,
                            sortMetric = releaseYearSortMetric,
                            onSortChange = { releaseYearSortMetric = it },
                            isAnime = isAnime,
                        )
                    }

                    if (mediumStats.startYears.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AniHyouWatchYearSection(
                            startYears = mediumStats.startYears,
                            sortMetric = watchYearSortMetric,
                            onSortChange = { watchYearSortMetric = it },
                            isAnime = isAnime,
                        )
                    }
                }
                AniHyouStatsCategory.GENRES -> {
                    AniHyouPositionalListSection(
                        title = "Genres",
                        items = mediumStats.genres.map { g ->
                            PositionalStatData(
                                name = g.genre,
                                count = g.count,
                                meanScore = g.meanScore,
                                minutesWatched = g.minutesWatched,
                                chaptersRead = g.chaptersRead,
                            )
                        },
                        sortMetric = generalSortMetric,
                        onSortChange = { generalSortMetric = it },
                        isAnime = isAnime,
                    )
                }
                AniHyouStatsCategory.TAGS -> {
                    AniHyouPositionalListSection(
                        title = "Tags",
                        items = mediumStats.tags.map { t ->
                            PositionalStatData(
                                name = t.name,
                                count = t.count,
                                meanScore = t.meanScore,
                                minutesWatched = t.minutesWatched,
                                chaptersRead = t.chaptersRead,
                            )
                        },
                        sortMetric = generalSortMetric,
                        onSortChange = { generalSortMetric = it },
                        isAnime = isAnime,
                    )
                }
                AniHyouStatsCategory.STAFF -> {
                    AniHyouPositionalListSection(
                        title = "Staff",
                        items = mediumStats.staff.map { s ->
                            PositionalStatData(
                                name = s.name,
                                count = s.count,
                                meanScore = s.meanScore,
                                minutesWatched = s.minutesWatched,
                                imageUrl = s.image,
                            )
                        },
                        sortMetric = generalSortMetric,
                        onSortChange = { generalSortMetric = it },
                        isAnime = isAnime,
                    )
                }
                AniHyouStatsCategory.VOICE_ACTORS -> {
                    AniHyouPositionalListSection(
                        title = "Voice Actors",
                        items = mediumStats.voiceActors.map { va ->
                            PositionalStatData(
                                name = va.name,
                                count = va.count,
                                meanScore = va.meanScore,
                                minutesWatched = va.minutesWatched,
                                imageUrl = va.image,
                            )
                        },
                        sortMetric = generalSortMetric,
                        onSortChange = { generalSortMetric = it },
                        isAnime = isAnime,
                    )
                }
                AniHyouStatsCategory.STUDIOS -> {
                    AniHyouPositionalListSection(
                        title = "Studios",
                        items = mediumStats.studios.map { st ->
                            PositionalStatData(
                                name = st.name,
                                count = st.count,
                                meanScore = st.meanScore,
                                minutesWatched = st.minutesWatched,
                            )
                        },
                        sortMetric = generalSortMetric,
                        onSortChange = { generalSortMetric = it },
                        isAnime = isAnime,
                    )
                }
            }
        }
    }
}

@Composable
private fun AniHyouMetricGrid(stats: AnilistMediumStatistics, isAnime: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricGridCell(value = "${stats.count}", label = "Total", modifier = Modifier.weight(1f))
            MetricGridCell(value = if (isAnime) formatNumberWithCommas(stats.episodesWatched) else formatNumberWithCommas(stats.chaptersRead), label = if (isAnime) "Episodes\nwatched" else "Chapters\nread", modifier = Modifier.weight(1f))
            MetricGridCell(value = "${stats.daysWatched.roundToInt()}", label = if (isAnime) "Days watched" else "Days read", modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricGridCell(value = "${stats.daysPlanned.roundToInt()}", label = "Days planned", modifier = Modifier.weight(1f))
            MetricGridCell(value = if (stats.meanScore > 0) "${stats.meanScore}" else "—", label = "Mean score", modifier = Modifier.weight(1f))
            MetricGridCell(value = if (stats.standardDeviation > 0) "${stats.standardDeviation}" else "—", label = "Standard\ndeviation", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricGridCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 21.sp), color = Color.White)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 15.sp), color = AniHyouSubtleText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AniHyouScoreSection(
    scores: List<AnilistScoreDistributionItem>,
    sortMetric: AniHyouStatSortMetric,
    onSortChange: (AniHyouStatSortMetric) -> Unit,
    isAnime: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = "Score", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AniHyouFilterChip(label = "Titles", selected = sortMetric == AniHyouStatSortMetric.TITLES, onClick = { onSortChange(AniHyouStatSortMetric.TITLES) })
            AniHyouFilterChip(label = "Time", selected = sortMetric == AniHyouStatSortMetric.TIME, onClick = { onSortChange(AniHyouStatSortMetric.TIME) })
        }
        val items = scores.map { item ->
            val value = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> item.count.toFloat()
                AniHyouStatSortMetric.TIME -> if (isAnime) (item.minutesWatched / 60f) else item.chaptersRead.toFloat()
                else -> item.count.toFloat()
            }
            val display = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> "${item.count}"
                AniHyouStatSortMetric.TIME -> "${(if (isAnime) item.minutesWatched / 60 else item.chaptersRead.toLong())}h"
                else -> "${item.count}"
            }
            VerticalStatItem(
                label = "${item.score}",
                value = value,
                displayValue = if (value > 0f) display else "",
                color = item.score.point100PrimaryColor(),
            )
        }
        AniHyouVerticalStatsBar(
            stats = items,
            mapColorTo = { it.color ?: it.label.toIntOrNull()?.point100PrimaryColor() ?: stat_dark_blue },
        )
    }
}

@Composable
private fun AniHyouLengthSection(
    lengths: List<AnilistLengthStatItem>,
    sortMetric: AniHyouStatSortMetric,
    onSortChange: (AniHyouStatSortMetric) -> Unit,
    isAnime: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = if (isAnime) "Episode Count" else "Chapter Count", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AniHyouFilterChip(label = "Titles", selected = sortMetric == AniHyouStatSortMetric.TITLES, onClick = { onSortChange(AniHyouStatSortMetric.TITLES) })
            AniHyouFilterChip(label = "Time", selected = sortMetric == AniHyouStatSortMetric.TIME, onClick = { onSortChange(AniHyouStatSortMetric.TIME) })
            AniHyouFilterChip(label = "Score", selected = sortMetric == AniHyouStatSortMetric.SCORE, onClick = { onSortChange(AniHyouStatSortMetric.SCORE) })
        }
        val items = lengths.map { item ->
            val value = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> item.count.toFloat()
                AniHyouStatSortMetric.TIME -> (item.minutesWatched / 60f)
                AniHyouStatSortMetric.SCORE -> item.meanScore.toFloat()
            }
            val display = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> "${item.count}"
                AniHyouStatSortMetric.TIME -> "${item.minutesWatched / 60}h"
                AniHyouStatSortMetric.SCORE -> if (item.meanScore > 0) "${item.meanScore.toInt()}%" else "—"
            }
            VerticalStatItem(
                label = item.length,
                value = value,
                displayValue = display,
                color = stat_dark_blue,
            )
        }
        AniHyouVerticalStatsBar(stats = items)
    }
}

@Composable
private fun AniHyouStatusSection(
    statuses: List<AnilistStatusStatItem>,
    isAnime: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Status Distribution", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        val items = statuses.map { st ->
            val label = when (st.status.uppercase()) {
                "CURRENT" -> if (isAnime) "Watching" else "Reading"
                "COMPLETED" -> "Completed"
                "PLANNING" -> if (isAnime) "Plan to Watch" else "Plan to Read"
                "PAUSED" -> "Paused"
                "DROPPED" -> "Dropped"
                "REPEATING" -> if (isAnime) "Rewatching" else "Rereading"
                else -> st.status
            }
            HorizontalStatItem(
                label = label,
                value = st.count.toFloat(),
                color = statusToPrimaryColor(st.status),
                onColor = statusToOnPrimaryColor(st.status),
            )
        }
        AniHyouHorizontalStatsBar(stats = items)
    }
}

@Composable
private fun AniHyouFormatSection(
    formats: List<AnilistFormatStatItem>,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Format Distribution", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        val items = formats.map { fmt ->
            HorizontalStatItem(
                label = fmt.format.replace("_", " "),
                value = fmt.count.toFloat(),
                color = formatToPrimaryColor(fmt.format),
                onColor = formatToOnPrimaryColor(fmt.format),
            )
        }
        AniHyouHorizontalStatsBar(stats = items)
    }
}

@Composable
private fun AniHyouCountrySection(
    countries: List<AnilistCountryStatItem>,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Country Distribution", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        val items = countries.map { cty ->
            HorizontalStatItem(
                label = cty.country,
                value = cty.count.toFloat(),
                color = countryToPrimaryColor(cty.country),
                onColor = countryToOnPrimaryColor(cty.country),
            )
        }
        AniHyouHorizontalStatsBar(stats = items)
    }
}

@Composable
private fun AniHyouReleaseYearSection(
    releaseYears: List<AnilistReleaseYearStatItem>,
    sortMetric: AniHyouStatSortMetric,
    onSortChange: (AniHyouStatSortMetric) -> Unit,
    isAnime: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Release Year", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AniHyouFilterChip(label = "Titles", selected = sortMetric == AniHyouStatSortMetric.TITLES, onClick = { onSortChange(AniHyouStatSortMetric.TITLES) })
            AniHyouFilterChip(label = "Time", selected = sortMetric == AniHyouStatSortMetric.TIME, onClick = { onSortChange(AniHyouStatSortMetric.TIME) })
            AniHyouFilterChip(label = "Score", selected = sortMetric == AniHyouStatSortMetric.SCORE, onClick = { onSortChange(AniHyouStatSortMetric.SCORE) })
        }
        val items = releaseYears.map { yr ->
            val value = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> yr.count.toFloat()
                AniHyouStatSortMetric.TIME -> (yr.minutesWatched / 60f)
                AniHyouStatSortMetric.SCORE -> yr.meanScore.toFloat()
            }
            val display = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> "${yr.count}"
                AniHyouStatSortMetric.TIME -> "${yr.minutesWatched / 60}h"
                AniHyouStatSortMetric.SCORE -> if (yr.meanScore > 0) "${yr.meanScore.toInt()}%" else "—"
            }
            VerticalStatItem(
                label = "${yr.releaseYear}",
                value = value,
                displayValue = display,
                color = yearToPrimaryColor(yr.releaseYear),
            )
        }
        AniHyouVerticalStatsBar(
            stats = items,
            mapColorTo = { it.color ?: yearToPrimaryColor(it.label.toIntOrNull() ?: 2020) },
        )
    }
}

@Composable
private fun AniHyouWatchYearSection(
    startYears: List<AnilistStartYearStatItem>,
    sortMetric: AniHyouStatSortMetric,
    onSortChange: (AniHyouStatSortMetric) -> Unit,
    isAnime: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = if (isAnime) "Watch Year" else "Read Year", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AniHyouFilterChip(label = "Titles", selected = sortMetric == AniHyouStatSortMetric.TITLES, onClick = { onSortChange(AniHyouStatSortMetric.TITLES) })
            AniHyouFilterChip(label = "Time", selected = sortMetric == AniHyouStatSortMetric.TIME, onClick = { onSortChange(AniHyouStatSortMetric.TIME) })
            AniHyouFilterChip(label = "Score", selected = sortMetric == AniHyouStatSortMetric.SCORE, onClick = { onSortChange(AniHyouStatSortMetric.SCORE) })
        }
        val items = startYears.map { yr ->
            val value = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> yr.count.toFloat()
                AniHyouStatSortMetric.TIME -> (yr.minutesWatched / 60f)
                AniHyouStatSortMetric.SCORE -> yr.meanScore.toFloat()
            }
            val display = when (sortMetric) {
                AniHyouStatSortMetric.TITLES -> "${yr.count}"
                AniHyouStatSortMetric.TIME -> "${yr.minutesWatched / 60}h"
                AniHyouStatSortMetric.SCORE -> if (yr.meanScore > 0) "${yr.meanScore.toInt()}%" else "—"
            }
            VerticalStatItem(
                label = "${yr.startYear}",
                value = value,
                displayValue = display,
                color = yearToPrimaryColor(yr.startYear),
            )
        }
        AniHyouVerticalStatsBar(
            stats = items,
            mapColorTo = { it.color ?: yearToPrimaryColor(it.label.toIntOrNull() ?: 2020) },
        )
    }
}

private data class PositionalStatData(
    val name: String,
    val count: Int,
    val meanScore: Double,
    val minutesWatched: Long = 0L,
    val chaptersRead: Int = 0,
    val imageUrl: String? = null,
)

@Composable
private fun AniHyouPositionalListSection(
    title: String,
    items: List<PositionalStatData>,
    sortMetric: AniHyouStatSortMetric,
    onSortChange: (AniHyouStatSortMetric) -> Unit,
    isAnime: Boolean,
) {
    val sorted = remember(items, sortMetric) {
        when (sortMetric) {
            AniHyouStatSortMetric.TITLES -> items.sortedByDescending { it.count }
            AniHyouStatSortMetric.TIME -> items.sortedByDescending { if (isAnime) it.minutesWatched else it.chaptersRead.toLong() }
            AniHyouStatSortMetric.SCORE -> items.sortedByDescending { it.meanScore }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AniHyouFilterChip(label = "Titles", selected = sortMetric == AniHyouStatSortMetric.TITLES, onClick = { onSortChange(AniHyouStatSortMetric.TITLES) })
            AniHyouFilterChip(label = "Time", selected = sortMetric == AniHyouStatSortMetric.TIME, onClick = { onSortChange(AniHyouStatSortMetric.TIME) })
            AniHyouFilterChip(label = "Score", selected = sortMetric == AniHyouStatSortMetric.SCORE, onClick = { onSortChange(AniHyouStatSortMetric.SCORE) })
        }

        if (sorted.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = "No $title stats recorded.", color = AniHyouSubtleText, fontSize = 14.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sorted.forEachIndexed { index, item ->
                    AniHyouPositionalStatItemView(
                        name = item.name,
                        position = index + 1,
                        count = item.count,
                        meanScore = item.meanScore,
                        minutesWatched = if (isAnime) item.minutesWatched else null,
                        chaptersRead = if (!isAnime) item.chaptersRead else null,
                        imageUrl = item.imageUrl,
                    )
                }
            }
        }
    }
}

@Composable
private fun AniHyouInfoTabContent(user: AnilistFullUserProfile) {
    val primaryColor = AniHyouCyanAccent
    val uriHandler = LocalUriHandler.current
    val bioBlocks = remember(user.about, primaryColor) { user.about?.let { parseAnilistRichContent(it, primaryColor) } ?: emptyList() }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (bioBlocks.isNotEmpty()) {
            bioBlocks.forEach { block -> AnilistContentBlockItem(block = block, primaryColor = primaryColor, uriHandler = uriHandler) }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "No bio provided.", style = MaterialTheme.typography.bodyMedium, color = AniHyouSubtleText)
            }
        }
    }
}

@Composable
private fun AniHyouActivityTabContent(userId: Int, onAnimeClick: ((Int) -> Unit)? = null) {
    var activities by remember { mutableStateOf<List<AnilistUserActivityItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(userId) {
        isLoading = true
        val res = AnilistProfileRepository.fetchUserActivity(userId)
        activities = res.getOrDefault(emptyList())
        isLoading = false
    }
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AniHyouCyanAccent, strokeWidth = 2.dp)
        }
    } else if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(text = "No recent activity.", style = MaterialTheme.typography.bodyMedium, color = AniHyouSubtleText)
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            activities.forEach { act ->
                Surface(modifier = Modifier.fillMaxWidth().clickable { onAnimeClick?.invoke(act.mediaId) }, shape = RoundedCornerShape(12.dp), color = AniHyouCardSurface, border = BorderStroke(1.dp, AniHyouChipBorder.copy(alpha = 0.5f))) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!act.coverImage.isNullOrBlank()) {
                            AsyncImage(model = act.coverImage, contentDescription = act.mediaTitle, modifier = Modifier.width(42.dp).aspectRatio(0.7f).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val actionText = when {
                                act.progress != null -> "${act.status} ${act.progress}"
                                else -> act.status
                            }

                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = AniHyouCyanAccent,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = act.mediaTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AniHyouFavoritesTabContent(
    user: AnilistFullUserProfile,
    onAnimeClick: ((Int) -> Unit)? = null,
) {
    var favoriteSubTab by remember { mutableStateOf("Anime") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AniHyouFilterChip(
                label = "Anime (${user.favoriteAnime.size})",
                selected = favoriteSubTab == "Anime",
                onClick = { favoriteSubTab = "Anime" },
            )
            AniHyouFilterChip(
                label = "Characters (${user.favoriteCharacters.size})",
                selected = favoriteSubTab == "Characters",
                onClick = { favoriteSubTab = "Characters" },
            )
        }

        when (favoriteSubTab) {
            "Anime" -> {
                if (user.favoriteAnime.isNotEmpty()) {
                    val chunkedAnime = remember(user.favoriteAnime) { user.favoriteAnime.chunked(3) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        chunkedAnime.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowItems.forEach { anime ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onAnimeClick?.invoke(anime.id) },
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.7f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AniHyouCardSurface),
                                        ) {
                                            if (!anime.coverImage.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = anime.coverImage,
                                                    contentDescription = anime.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            }
                                            if (anime.averageScore != null && anime.averageScore > 0) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.78f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFB800),
                                                            modifier = Modifier.size(10.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "${anime.averageScore}%",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                            color = Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Text(
                                            text = anime.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No favorite anime.", color = AniHyouSubtleText)
                    }
                }
            }

            "Characters" -> {
                if (user.favoriteCharacters.isNotEmpty()) {
                    val chunkedChars = remember(user.favoriteCharacters) { user.favoriteCharacters.chunked(3) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        chunkedChars.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowItems.forEach { char ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(CircleShape)
                                                .background(AniHyouCardSurface),
                                        ) {
                                            if (!char.image.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = char.image,
                                                    contentDescription = char.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Text(
                                            text = char.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No favorite characters.", color = AniHyouSubtleText)
                    }
                }
            }
        }
    }
}

@Composable
private fun AniHyouSocialTabContent(
    userId: Int,
) {
    var socialSubTab by remember { mutableStateOf("Following") }
    var users by remember { mutableStateOf<List<AnilistSocialUserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId, socialSubTab) {
        isLoading = true
        val res = if (socialSubTab == "Following") {
            AnilistProfileRepository.fetchFollowing(userId)
        } else {
            AnilistProfileRepository.fetchFollowers(userId)
        }
        users = res.getOrDefault(emptyList())
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AniHyouFilterChip(
                label = "Following",
                selected = socialSubTab == "Following",
                onClick = { socialSubTab = "Following" },
            )
            AniHyouFilterChip(
                label = "Followers",
                selected = socialSubTab == "Followers",
                onClick = { socialSubTab = "Followers" },
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AniHyouCyanAccent, strokeWidth = 2.dp)
            }
        } else if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No $socialSubTab found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AniHyouSubtleText,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                users.forEach { u ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = AniHyouCardSurface,
                        border = BorderStroke(1.dp, AniHyouChipBorder.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AniHyouChipActiveBg),
                            ) {
                                if (!u.avatar.isNullOrBlank()) {
                                    AsyncImage(
                                        model = u.avatar,
                                        contentDescription = u.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = u.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                if (!u.donatorBadge.isNullOrBlank()) {
                                    Text(
                                        text = u.donatorBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AniHyouCyanAccent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatNumberWithCommas(value: Int): String {
    val str = value.toString()
    val sb = StringBuilder()
    for (i in str.indices) {
        if (i > 0 && (str.length - i) % 3 == 0) {
            sb.append(',')
        }
        sb.append(str[i])
    }
    return sb.toString()
}
