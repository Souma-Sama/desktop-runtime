package com.nuvio.app.features.anilist.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.community.AnilistCommunityRepository
import com.nuvio.app.features.anilist.community.AnilistReview
import com.nuvio.app.features.anilist.community.AnilistReviewVote
import com.nuvio.app.features.anilist.community.ReviewDetailSheet
import com.nuvio.app.features.anilist.community.WriteReviewDialog
import com.nuvio.app.features.anilist.profile.AnilistUserProfileSheet
import com.nuvio.app.features.anilist.recommendations.AnilistRecommendation
import com.nuvio.app.features.anilist.recommendations.AnilistRecommendationsRepository
import com.nuvio.app.features.anilist.threads.AnilistThread
import com.nuvio.app.features.anilist.threads.AnilistThreadsRepository
import com.nuvio.app.features.anilist.threads.ThreadDetailSheet
import com.nuvio.app.features.anilist.ui.components.AnilistDiscussionCard
import com.nuvio.app.features.anilist.ui.components.AnilistRecommendationCard
import com.nuvio.app.features.anilist.ui.components.AnilistReviewCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun AnilistCommunityHub(
    mediaId: Int,
    animeTitle: String,
    modifier: Modifier = Modifier,
    horizontalScrollPadding: Dp = 16.dp,
    onAnimeClick: ((Int) -> Unit)? = null,
) {
    var selectedTab by remember(mediaId) { mutableStateOf(AnilistHubTab.DISCUSSIONS) }

    // Discussions State
    var threads by remember(mediaId) { mutableStateOf<List<AnilistThread>>(emptyList()) }
    var isLoadingThreads by remember(mediaId) { mutableStateOf(true) }

    // Reviews State
    var reviews by remember(mediaId) { mutableStateOf<List<AnilistReview>>(emptyList()) }
    var isLoadingReviews by remember(mediaId) { mutableStateOf(true) }
    var totalReviews by remember(mediaId) { mutableStateOf(0) }
    var hasNextReviewPage by remember(mediaId) { mutableStateOf(false) }
    var currentReviewPage by remember(mediaId) { mutableStateOf(1) }
    var isLoadingMoreReviews by remember(mediaId) { mutableStateOf(false) }

    // Recommendations State
    var recommendations by remember(mediaId) { mutableStateOf<List<AnilistRecommendation>>(emptyList()) }
    var isLoadingRecommendations by remember(mediaId) { mutableStateOf(true) }

    // Modal Sheet States
    var selectedThread by remember { mutableStateOf<AnilistThread?>(null) }
    var selectedReview by remember { mutableStateOf<AnilistReview?>(null) }
    var showWriteReviewDialog by remember { mutableStateOf(false) }
    var reviewToEdit by remember { mutableStateOf<AnilistReview?>(null) }
    var profileToViewId by remember { mutableStateOf<Int?>(null) }
    var profileToViewName by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val isLoggedIn = AnilistAuthRepository.isAuthenticated.value
    val currentUserId = AnilistAuthRepository.currentUser.value?.id
    val userReview = reviews.firstOrNull { it.userId == currentUserId }

    // Lazy list states for smooth scrolling
    val threadListState = rememberLazyListState()
    val reviewListState = rememberLazyListState()
    val recListState = rememberLazyListState()

    // 1. Fetch Discussions
    LaunchedEffect(mediaId) {
        isLoadingThreads = true
        val result = AnilistThreadsRepository.getMediaThreads(mediaId = mediaId)
        result.onSuccess {
            threads = it.threads
            isLoadingThreads = false
        }.onFailure {
            isLoadingThreads = false
        }
    }

    // 2. Fetch Reviews
    LaunchedEffect(mediaId) {
        isLoadingReviews = true
        try {
            val pageResult = AnilistCommunityRepository.getReviewsPage(mediaId = mediaId, page = 1)
            reviews = pageResult.items
            hasNextReviewPage = pageResult.hasNextPage
            currentReviewPage = pageResult.page
            totalReviews = pageResult.total
        } catch (_: Exception) {
        } finally {
            isLoadingReviews = false
        }
    }

    // 3. Fetch Recommendations
    LaunchedEffect(mediaId) {
        isLoadingRecommendations = true
        val result = AnilistRecommendationsRepository.getMediaRecommendations(mediaId = mediaId)
        result.onSuccess {
            recommendations = it.recommendations
            isLoadingRecommendations = false
        }.onFailure {
            isLoadingRecommendations = false
        }
    }

    // Reviews pagination trigger
    LaunchedEffect(reviewListState, hasNextReviewPage, isLoadingMoreReviews, isLoadingReviews) {
        if (isLoadingReviews) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = reviewListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = reviewListState.layoutInfo.totalItemsCount
            hasNextReviewPage && !isLoadingMoreReviews && total > 0 && lastVisible >= total - 2
        }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad) {
                    isLoadingMoreReviews = true
                    try {
                        val next = currentReviewPage + 1
                        val pageResult = AnilistCommunityRepository.getReviewsPage(mediaId = mediaId, page = next)
                        reviews = (reviews + pageResult.items).distinctBy { it.id }
                        hasNextReviewPage = pageResult.hasNextPage
                        currentReviewPage = next
                    } catch (_: Exception) {
                    } finally {
                        isLoadingMoreReviews = false
                    }
                }
            }
    }

    // Hide entire section if nothing exists and loading finished
    val hasAnyData = threads.isNotEmpty() || reviews.isNotEmpty() || recommendations.isNotEmpty()
    val isAnyLoading = isLoadingThreads || isLoadingReviews || isLoadingRecommendations
    if (!hasAnyData && !isAnyLoading) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        // Header with Segmented Pill Switcher & Context Action
        AnilistHubHeader(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            discussionCount = threads.size,
            reviewCount = if (totalReviews > 0) totalReviews else reviews.size,
            recommendationCount = recommendations.size,
            isLoggedIn = isLoggedIn,
            userHasReview = userReview != null,
            onWriteReviewClick = {
                reviewToEdit = userReview
                showWriteReviewDialog = true
            },
            horizontalPadding = horizontalScrollPadding,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Shelf Content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "HubShelfTransition",
        ) { targetTab ->
            when (targetTab) {
                AnilistHubTab.DISCUSSIONS -> {
                    if (isLoadingThreads) {
                        HubLoadingState()
                    } else if (threads.isEmpty()) {
                        HubEmptyState(text = "No community discussions found for this anime.")
                    } else {
                        LazyRow(
                            state = threadListState,
                            contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .nuvioDesktopDragScroll(threadListState),
                        ) {
                            items(threads, key = { "thread_${it.id}" }) { thread ->
                                AnilistDiscussionCard(
                                    thread = thread,
                                    onClick = { selectedThread = thread },
                                    onAuthorClick = {
                                        thread.user?.let { u ->
                                            profileToViewId = u.id
                                            profileToViewName = u.name
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                AnilistHubTab.REVIEWS -> {
                    if (isLoadingReviews) {
                        HubLoadingState()
                    } else if (reviews.isEmpty()) {
                        HubEmptyState(text = "No reviews written yet. Be the first to review!")
                    } else {
                        LazyRow(
                            state = reviewListState,
                            contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .nuvioDesktopDragScroll(reviewListState),
                        ) {
                            items(reviews, key = { "rev_${it.id}" }) { review ->
                                AnilistReviewCard(
                                    review = review,
                                    onClick = { selectedReview = review },
                                    onAuthorClick = {
                                        review.user?.let { u ->
                                            profileToViewId = u.id
                                            profileToViewName = u.name
                                        }
                                    },
                                )
                            }

                            if (isLoadingMoreReviews) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 100.dp, height = 180.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AnilistHubTab.RECOMMENDATIONS -> {
                    if (isLoadingRecommendations) {
                        HubLoadingState()
                    } else if (recommendations.isEmpty()) {
                        HubEmptyState(text = "No community recommendations available.")
                    } else {
                        LazyRow(
                            state = recListState,
                            contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .nuvioDesktopDragScroll(recListState),
                        ) {
                            items(recommendations, key = { "rec_${it.id}" }) { rec ->
                                AnilistRecommendationCard(
                                    recommendation = rec,
                                    onClick = {
                                        onAnimeClick?.invoke(rec.mediaId)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Discussion Reader
    selectedThread?.let { thread ->
        ThreadDetailSheet(
            thread = thread,
            onDismiss = { selectedThread = null },
        )
    }

    // Modal: Review Reader
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
                        selectedReview = selectedReview?.let {
                            if (it.id == rev.id) it.copy(rating = updated.rating, ratingAmount = updated.ratingAmount, userRating = updated.userRating) else it
                        }
                    }
                }
            },
            onEdit = { rev ->
                selectedReview = null
                reviewToEdit = rev
                showWriteReviewDialog = true
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

    // Modal: Write / Edit Review Dialog
    if (showWriteReviewDialog) {
        WriteReviewDialog(
            mediaId = mediaId,
            animeTitle = animeTitle,
            existingReview = reviewToEdit,
            onDismiss = {
                showWriteReviewDialog = false
                reviewToEdit = null
            },
            onReviewSaved = { saved ->
                val withoutExisting = reviews.filter { it.id != saved.id }
                reviews = listOf(saved) + withoutExisting
                totalReviews += 1
            },
        )
    }

    // Modal: User Profile Sheet
    if (profileToViewId != null || profileToViewName != null) {
        AnilistUserProfileSheet(
            userId = profileToViewId,
            username = profileToViewName,
            onDismiss = {
                profileToViewId = null
                profileToViewName = null
            },
            onAnimeClick = onAnimeClick,
        )
    }
}

@Composable
private fun HubLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HubEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
