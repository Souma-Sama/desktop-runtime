package com.nuvio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.AppScreenTab
import com.nuvio.app.core.time.EpisodeReleaseDatePlatform
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.profiles.ActiveProfileMiniAvatar
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.SidebarProfileSwitcherStack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_nav_home
import nuvio.composeapp.generated.resources.compose_nav_library
import nuvio.composeapp.generated.resources.compose_nav_profile
import nuvio.composeapp.generated.resources.compose_nav_search
import nuvio.composeapp.generated.resources.compose_settings_page_root
import nuvio.composeapp.generated.resources.sidebar_library
import nuvio.composeapp.generated.resources.sidebar_search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val FloatingGlassSidebarCollapsedWidth = 58.dp
internal val FloatingGlassSidebarExpandedWidth = 200.dp

@Composable
internal fun FloatingGlassDesktopSidebar(
    selectedTab: AppScreenTab,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    sidebarExpanded: Boolean = false,
    sidebarWidth: Dp = FloatingGlassSidebarCollapsedWidth,
    hoverSource: MutableInteractionSource = remember { MutableInteractionSource() },
    profileStackVisible: Boolean = false,
    onProfileStackVisibleChange: (Boolean) -> Unit = {},
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    val activeProfile = profileState.activeProfile
    val activeProfileName = activeProfile?.name ?: stringResource(Res.string.compose_nav_profile)
    val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsStateWithLifecycle()

    var currentTimeStr by remember {
        mutableStateOf(
            EpisodeReleaseDatePlatform.localTimeAtEpochMs(EpisodeReleaseDatePlatform.nowEpochMs()) ?: "00:00"
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeStr = EpisodeReleaseDatePlatform.localTimeAtEpochMs(EpisodeReleaseDatePlatform.nowEpochMs()) ?: "00:00"
            delay(15_000L)
        }
    }

    fun selectTab(tab: AppScreenTab) {
        onProfileStackVisibleChange(false)
        onTabSelected(tab)
    }

    val glassShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .padding(start = 12.dp)
            .width(sidebarWidth)
            .wrapContentHeight()
            .hoverable(hoverSource)
            .zIndex(NuvioTokens.Z.navigation),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(
                    elevation = 20.dp,
                    shape = glassShape,
                    spotColor = Color.Black.copy(alpha = 0.50f),
                    ambientColor = Color.Black.copy(alpha = 0.30f),
                )
                .clip(glassShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = 28.dp
                            noiseFactor = 0.05f
                        }
                    } else {
                        Modifier
                    },
                )
                .background(
                    if (hazeState != null) {
                        Color(0xFF14151C).copy(alpha = 0.52f) // Liquid glass translucency
                    } else {
                        Color(0xFF14151C).copy(alpha = 0.88f)
                    },
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = glassShape,
                ),
        ) {
            // Subtle liquid glass top highlight sheen
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(glassShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 8.dp),
            ) {
                // --- 1. TOP HEADER: Profile Avatar + Name + Live Clock ---
                val headerInteractionSource = remember { MutableInteractionSource() }
                val isHeaderHovered by headerInteractionSource.collectIsHoveredAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isHeaderHovered) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable(
                            interactionSource = headerInteractionSource,
                            indication = null,
                            onClick = { onProfileStackVisibleChange(!profileStackVisible) },
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        ActiveProfileMiniAvatar(
                            profile = activeProfile,
                            avatars = avatars,
                            selected = false,
                            size = 32,
                        )
                    }

                    AnimatedVisibility(
                        visible = sidebarExpanded,
                        enter = fadeIn(animationSpec = tween(160)) + expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                        ),
                        exit = shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                        ) + fadeOut(
                            animationSpec = tween(80, delayMillis = 120),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = activeProfileName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )

                            Text(
                                text = currentTimeStr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.55f),
                                maxLines = 1,
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.height(6.dp))

                // --- 2. NAVIGATION ITEMS (Liquid Pill Selection with Spring Motion) ---
                val navTabs = remember(anilistPrefs.enabled) {
                    listOfNotNull(
                        AppScreenTab.Search,
                        AppScreenTab.Home,
                        if (anilistPrefs.enabled) AppScreenTab.Explore else null,
                        AppScreenTab.Library,
                        AppScreenTab.Settings,
                    )
                }
                val selectedTabIndex = navTabs.indexOf(selectedTab)
                val hasValidSelection = selectedTabIndex >= 0

                val animatedPillOffsetY by animateDpAsState(
                    targetValue = if (hasValidSelection) (selectedTabIndex * 44).dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "floating_sidebar_pill_y",
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    if (hasValidSelection) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .offset(y = animatedPillOffsetY)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = Color.White.copy(alpha = 0.35f),
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FloatingGlassSidebarItem(
                            label = stringResource(Res.string.compose_nav_search),
                            selected = selectedTab == AppScreenTab.Search,
                            expanded = sidebarExpanded,
                            onClick = { selectTab(AppScreenTab.Search) },
                        ) { color ->
                            Icon(
                                painter = painterResource(Res.drawable.sidebar_search),
                                contentDescription = stringResource(Res.string.compose_nav_search),
                                modifier = Modifier.size(20.dp),
                                tint = color,
                            )
                        }

                        FloatingGlassSidebarItem(
                            label = stringResource(Res.string.compose_nav_home),
                            selected = selectedTab == AppScreenTab.Home,
                            expanded = sidebarExpanded,
                            onClick = { selectTab(AppScreenTab.Home) },
                        ) { color ->
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = stringResource(Res.string.compose_nav_home),
                                modifier = Modifier.size(20.dp),
                                tint = color,
                            )
                        }

                        if (anilistPrefs.enabled) {
                            FloatingGlassSidebarItem(
                                label = "Explore",
                                selected = selectedTab == AppScreenTab.Explore,
                                expanded = sidebarExpanded,
                                onClick = { selectTab(AppScreenTab.Explore) },
                            ) { color ->
                                Icon(
                                    imageVector = com.nuvio.app.features.anilist.ExploreIconVector,
                                    contentDescription = "Explore",
                                    modifier = Modifier.size(20.dp),
                                    tint = color,
                                )
                            }
                        }

                        FloatingGlassSidebarItem(
                            label = stringResource(Res.string.compose_nav_library),
                            selected = selectedTab == AppScreenTab.Library,
                            expanded = sidebarExpanded,
                            onClick = { selectTab(AppScreenTab.Library) },
                        ) { color ->
                            Icon(
                                painter = painterResource(Res.drawable.sidebar_library),
                                contentDescription = stringResource(Res.string.compose_nav_library),
                                modifier = Modifier.size(20.dp),
                                tint = color,
                            )
                        }

                        FloatingGlassSidebarItem(
                            label = stringResource(Res.string.compose_settings_page_root),
                            selected = selectedTab == AppScreenTab.Settings,
                            expanded = sidebarExpanded,
                            onClick = { selectTab(AppScreenTab.Settings) },
                        ) { color ->
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(Res.string.compose_settings_page_root),
                                modifier = Modifier.size(20.dp),
                                tint = color,
                            )
                        }
                    }
                }
            }
        }

        // Profile switcher popover if opened
        if (profileStackVisible) {
            SidebarProfileSwitcherStack(
                onProfileSelected = onProfileSelected,
                onAddProfileRequested = onAddProfileRequested,
                onDismissRequest = { onProfileStackVisibleChange(false) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 56.dp, start = 8.dp)
                    .width(180.dp)
                    .zIndex(NuvioTokens.Z.sheet),
            )
        }
    }
}

@Composable
private fun FloatingGlassSidebarItem(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> Color.Black
            isHovered -> Color.White
            else -> Color.White.copy(alpha = 0.72f)
        },
        animationSpec = tween(180),
        label = "sidebar_item_content_color",
    )

    val itemBackgroundColor = when {
        isHovered && !selected -> Color.White.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        color = itemBackgroundColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon(contentColor)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(160)) + expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ),
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ) + fadeOut(
                    animationSpec = tween(80, delayMillis = 120),
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                        ),
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
