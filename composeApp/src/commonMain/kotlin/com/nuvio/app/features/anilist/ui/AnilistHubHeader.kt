package com.nuvio.app.features.anilist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvioDesktopDragScroll

enum class AnilistHubTab(
    val title: String,
    val icon: ImageVector,
) {
    DISCUSSIONS("Discussions", Icons.Default.Forum),
    REVIEWS("Reviews", Icons.Default.Star),
    RECOMMENDATIONS("Recommendations", Icons.Default.Lightbulb),
    RELATED("Related", Icons.Default.AccountTree),
}

@Composable
fun AnilistHubHeader(
    selectedTab: AnilistHubTab,
    onTabSelected: (AnilistHubTab) -> Unit,
    discussionCount: Int = 0,
    reviewCount: Int = 0,
    recommendationCount: Int = 0,
    relatedCount: Int = 0,
    isLoggedIn: Boolean = false,
    userHasReview: Boolean = false,
    onWriteReviewClick: () -> Unit = {},
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        val isCompact = maxWidth < 680.dp

        if (!isCompact) {
            // Wide Layout: Tabs on left, Action on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Segmented Pill Container
                SegmentedTabsContainer(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    discussionCount = discussionCount,
                    reviewCount = reviewCount,
                    recommendationCount = recommendationCount,
                    relatedCount = relatedCount,
                    modifier = Modifier.wrapContentWidth(),
                )

                // Context Action Button
                if (selectedTab == AnilistHubTab.REVIEWS && isLoggedIn) {
                    WriteReviewActionButton(
                        userHasReview = userHasReview,
                        onClick = onWriteReviewClick,
                    )
                }
            }
        } else {
            // Compact / Mobile Portrait Layout: Scrollable tabs with action button below or aligned
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .nuvioDesktopDragScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SegmentedTabsContainer(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        discussionCount = discussionCount,
                        reviewCount = reviewCount,
                        recommendationCount = recommendationCount,
                        relatedCount = relatedCount,
                    )

                    if (selectedTab == AnilistHubTab.REVIEWS && isLoggedIn) {
                        WriteReviewActionButton(
                            userHasReview = userHasReview,
                            onClick = onWriteReviewClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabsContainer(
    selectedTab: AnilistHubTab,
    onTabSelected: (AnilistHubTab) -> Unit,
    discussionCount: Int,
    reviewCount: Int,
    recommendationCount: Int,
    relatedCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HubTabPill(
                tab = AnilistHubTab.DISCUSSIONS,
                isSelected = selectedTab == AnilistHubTab.DISCUSSIONS,
                count = discussionCount,
                onClick = { onTabSelected(AnilistHubTab.DISCUSSIONS) },
            )
            HubTabPill(
                tab = AnilistHubTab.REVIEWS,
                isSelected = selectedTab == AnilistHubTab.REVIEWS,
                count = reviewCount,
                onClick = { onTabSelected(AnilistHubTab.REVIEWS) },
            )
            HubTabPill(
                tab = AnilistHubTab.RECOMMENDATIONS,
                isSelected = selectedTab == AnilistHubTab.RECOMMENDATIONS,
                count = recommendationCount,
                onClick = { onTabSelected(AnilistHubTab.RECOMMENDATIONS) },
            )
            if (relatedCount > 0) {
                HubTabPill(
                    tab = AnilistHubTab.RELATED,
                    isSelected = selectedTab == AnilistHubTab.RELATED,
                    count = relatedCount,
                    onClick = { onTabSelected(AnilistHubTab.RELATED) },
                )
            }
        }
    }
}

@Composable
private fun WriteReviewActionButton(
    userHasReview: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (userHasReview) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        ),
        modifier = modifier.wrapContentWidth(),
    ) {
        Icon(
            imageVector = if (userHasReview) Icons.Default.Add else Icons.Outlined.RateReview,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (userHasReview) "Edit Your Review" else "Write Review",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun HubTabPill(
    tab: AnilistHubTab,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val containerColor = if (isSelected) activeColor else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                color = contentColor,
                maxLines = 1,
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
        }
    }
}
