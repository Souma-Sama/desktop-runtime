package com.nuvio.app.features.anilist.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.nuvioShelfHoverOverdraw
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.details.components.DetailRailHoverInset
import com.nuvio.app.features.details.components.DetailSectionHeader
import com.nuvio.app.isDesktop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun DetailAnilistReviewsSection(
    mediaId: Int,
    animeTitle: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    headerHorizontalPadding: Dp = 0.dp,
    horizontalScrollPadding: Dp = 0.dp,
    onLoginClick: (() -> Unit)? = null,
) {
    var reviews by remember(mediaId) { mutableStateOf<List<AnilistReview>>(emptyList()) }
    var isLoading by remember(mediaId) { mutableStateOf(true) }
    var isLoadingMore by remember(mediaId) { mutableStateOf(false) }
    var hasNextPage by remember(mediaId) { mutableStateOf(false) }
    var currentPage by remember(mediaId) { mutableStateOf(1) }
    var totalReviews by remember(mediaId) { mutableStateOf(0) }
    var selectedReview by remember { mutableStateOf<AnilistReview?>(null) }
    var showWriteDialog by remember { mutableStateOf(false) }
    var reviewToEdit by remember { mutableStateOf<AnilistReview?>(null) }

    val scope = rememberCoroutineScope()
    val isLoggedIn = AnilistAuthRepository.isLoggedIn
    val currentUserId = AnilistAuthRepository.currentUser?.id
    val userReview = reviews.firstOrNull { it.userId == currentUserId }

    // Lazy load reviews on first mount
    LaunchedEffect(mediaId) {
        isLoading = true
        try {
            val pageResult = AnilistCommunityRepository.getReviewsPage(mediaId = mediaId, page = 1)
            reviews = pageResult.items
            hasNextPage = pageResult.hasNextPage
            currentPage = pageResult.page
            totalReviews = pageResult.total
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    val listState = rememberLazyListState()

    // Pagination trigger
    LaunchedEffect(listState, hasNextPage, isLoadingMore, isLoading) {
        if (isLoading) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            hasNextPage && !isLoadingMore && total > 0 && lastVisible >= total - 2
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    isLoadingMore = true
                    try {
                        val next = currentPage + 1
                        val pageResult = AnilistCommunityRepository.getReviewsPage(mediaId = mediaId, page = next)
                        reviews = (reviews + pageResult.items).distinctBy { it.id }
                        hasNextPage = pageResult.hasNextPage
                        currentPage = next
                    } catch (_: Exception) {
                    } finally {
                        isLoadingMore = false
                    }
                }
            }
    }

    val rowHoverInset = if (isDesktop) DetailRailHoverInset else 0.dp
    val rowEdgePadding = headerHorizontalPadding + horizontalScrollPadding + rowHoverInset

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isDesktop) Modifier.nuvioShelfHoverOverdraw(rowHoverInset) else Modifier
            ),
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = headerHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailSectionHeader(
                        title = "Community Reviews",
                        modifier = Modifier,
                    )
                    if (totalReviews > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = "$totalReviews",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                if (isLoggedIn) {
                    Button(
                        onClick = {
                            reviewToEdit = userReview
                            showWriteDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = if (userReview != null) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (userReview != null) "Your Review" else "Write Review",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                } else if (onLoginClick != null) {
                    OutlinedButton(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Log In to Review", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when {
            isLoading -> {
                val loadingListState = rememberLazyListState()
                LazyRow(
                    state = loadingListState,
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth()
                        .nuvioDesktopDragScroll(loadingListState),
                    contentPadding = PaddingValues(start = rowEdgePadding, end = rowEdgePadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(3) {
                        LoadingReviewCard()
                    }
                }
            }

            reviews.isEmpty() -> {
                ReviewEmptyState(
                    isLoggedIn = isLoggedIn,
                    onWriteClick = {
                        reviewToEdit = null
                        showWriteDialog = true
                    },
                    onLoginClick = onLoginClick,
                    modifier = Modifier.padding(horizontal = headerHorizontalPadding),
                )
            }

            else -> {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth()
                        .nuvioDesktopDragScroll(listState),
                    contentPadding = PaddingValues(
                        start = rowEdgePadding,
                        top = rowHoverInset,
                        end = rowEdgePadding,
                        bottom = rowHoverInset,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(
                        items = reviews,
                        key = { "rev_${it.id}" },
                    ) { review ->
                        ReviewCard(
                            review = review,
                            onClick = { selectedReview = review },
                        )
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(280.dp)
                                    .height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Sheet Modal
    selectedReview?.let { review ->
        ReviewDetailSheet(
            review = review,
            onDismiss = { selectedReview = null },
            onVote = { rev, vote ->
                scope.launch {
                    val result = AnilistCommunityRepository.voteReview(
                        reviewId = rev.id,
                        rating = vote,
                        mediaId = mediaId,
                    )
                    result.onSuccess { updated ->
                        reviews = reviews.map { if (it.id == rev.id) it.copy(rating = updated.rating, ratingAmount = updated.ratingAmount, userRating = updated.userRating) else it }
                    }
                }
            },
            onEdit = { rev ->
                selectedReview = null
                reviewToEdit = rev
                showWriteDialog = true
            },
            onDelete = { rev ->
                scope.launch {
                    val result = AnilistCommunityRepository.deleteReview(reviewId = rev.id, mediaId = mediaId)
                    result.onSuccess {
                        reviews = reviews.filter { it.id != rev.id }
                        totalReviews = (totalReviews - 1).coerceAtLeast(0)
                        selectedReview = null
                    }
                }
            },
        )
    }

    // Write / Edit Review Dialog
    if (showWriteDialog) {
        WriteReviewDialog(
            mediaId = mediaId,
            animeTitle = animeTitle,
            existingReview = reviewToEdit,
            onDismiss = {
                showWriteDialog = false
                reviewToEdit = null
            },
            onReviewSaved = { saved ->
                val withoutExisting = reviews.filter { it.id != saved.id }
                reviews = listOf(saved) + withoutExisting
                totalReviews += 1
            },
        )
    }
}

@Composable
private fun ReviewCard(
    review: AnilistReview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(320.dp)
            .height(204.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top: User Info & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val avatarUrl = review.user?.avatarMedium ?: review.user?.avatarLarge
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = review.user.name,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = review.user?.name?.take(1)?.uppercase() ?: "A",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Column {
                        Text(
                            text = review.user?.name ?: "Anonymous",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatReviewDateShort(review.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Score Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (review.score >= 75) Color(0xFF10B981)
                            else if (review.score >= 50) Color(0xFFF59E0B)
                            else Color(0xFFEF4444)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${review.score}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                        ),
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Summary & Snippet
            Column(modifier = Modifier.weight(1f)) {
                if (review.summary.isNotBlank()) {
                    Text(
                        text = review.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = cleanSnippet(review.body),
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (review.summary.isNotBlank()) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom: Helpful Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${review.rating} helpful",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "Read more →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LoadingReviewCard() {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(204.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun ReviewEmptyState(
    isLoggedIn: Boolean,
    onWriteClick: () -> Unit,
    onLoginClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.RateReview,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "No community reviews yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Be the first in the AniList community to share your thoughts on this anime!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isLoggedIn) {
                Button(
                    onClick = onWriteClick,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write a Review")
                }
            } else if (onLoginClick != null) {
                OutlinedButton(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Log in with AniList")
                }
            }
        }
    }
}

private fun cleanSnippet(body: String): String {
    return body
        .replace(Regex("<img[^>]*>"), "")
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        .replace(Regex("_{1,2}([^_]+)_{1,2}"), "$1")
        .replace(Regex("\\*{1,2}([^*]+)\\*{1,2}"), "$1")
        .replace("~!", "")
        .replace("!~", "")
        .trim()
}

private fun formatReviewDateShort(timestampSeconds: Long): String {
    if (timestampSeconds <= 0L) return ""
    val now = io.ktor.util.date.GMTDate().timestamp / 1000
    val diffSecs = (now - timestampSeconds).coerceAtLeast(0L)
    val days = diffSecs / 86400
    val months = days / 30
    val years = days / 365

    return when {
        years > 0 -> "${years}y ago"
        months > 0 -> "${months}mo ago"
        days > 0 -> "${days}d ago"
        diffSecs >= 3600 -> "${diffSecs / 3600}h ago"
        else -> "Recently"
    }
}
