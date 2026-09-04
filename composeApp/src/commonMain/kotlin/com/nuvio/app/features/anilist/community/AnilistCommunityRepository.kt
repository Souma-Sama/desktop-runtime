package com.nuvio.app.features.anilist.community

import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnilistCommunityRepository {

    private val reviewsCache = mutableMapOf<Int, AnilistReviewPage>()

    suspend fun getReviewsPage(
        mediaId: Int,
        page: Int = 1,
        perPage: Int = 12,
        forceRefresh: Boolean = false,
    ): AnilistReviewPage = withContext(Dispatchers.Default) {
        if (!forceRefresh && page == 1) {
            reviewsCache[mediaId]?.let { return@withContext it }
        }

        val token = AnilistAuthRepository.token.value
        val pageResult = AnilistApi.fetchMediaReviews(
            mediaId = mediaId,
            page = page,
            perPage = perPage,
            token = token,
        )

        if (page == 1) {
            reviewsCache[mediaId] = pageResult
        } else {
            val existing = reviewsCache[mediaId]
            if (existing != null) {
                val combined = (existing.items + pageResult.items).distinctBy { it.id }
                reviewsCache[mediaId] = pageResult.copy(items = combined)
            }
        }

        pageResult
    }

    suspend fun voteReview(
        reviewId: Int,
        rating: AnilistReviewVote,
        mediaId: Int,
    ): Result<AnilistReview> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to rate reviews"))

        val ratingString = when (rating) {
            AnilistReviewVote.NO_VOTE, AnilistReviewVote.NO_RATING -> "NO_VOTE"
            AnilistReviewVote.UP_VOTE -> "UP_VOTE"
            AnilistReviewVote.DOWN_VOTE -> "DOWN_VOTE"
        }

        val updated = AnilistApi.rateReview(
            reviewId = reviewId,
            rating = ratingString,
            token = token,
        ) ?: return@withContext Result.failure(Exception("Failed to update rating"))

        // Update in-memory cache
        val cached = reviewsCache[mediaId]
        if (cached != null) {
            val updatedItems = cached.items.map { rev ->
                if (rev.id == reviewId) {
                    rev.copy(
                        rating = updated.rating,
                        ratingAmount = updated.ratingAmount,
                        userRating = updated.userRating,
                    )
                } else rev
            }
            reviewsCache[mediaId] = cached.copy(items = updatedItems)
        }

        Result.success(updated)
    }

    suspend fun publishReview(
        mediaId: Int,
        summary: String,
        body: String,
        score: Int,
        reviewId: Int? = null,
    ): Result<AnilistReview> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to post a review"))

        val cleanSummary = summary.trim()
        val cleanBody = body.trim()

        if (cleanSummary.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Summary must be at least 5 characters"))
        }
        if (cleanBody.length < 50) {
            return@withContext Result.failure(IllegalArgumentException("Review body must be at least 50 characters"))
        }
        if (score !in 0..100) {
            return@withContext Result.failure(IllegalArgumentException("Score must be between 0 and 100"))
        }

        val saved = AnilistApi.saveReview(
            mediaId = mediaId,
            summary = cleanSummary,
            body = cleanBody,
            score = score,
            token = token,
            reviewId = reviewId,
        ) ?: return@withContext Result.failure(Exception("Failed to save review on AniList"))

        // Invalidate or prepend to cache
        val cached = reviewsCache[mediaId]
        if (cached != null) {
            val withoutExisting = cached.items.filter { it.id != saved.id }
            val updatedList = listOf(saved) + withoutExisting
            reviewsCache[mediaId] = cached.copy(items = updatedList, total = cached.total + 1)
        }

        Result.success(saved)
    }

    suspend fun deleteReview(
        reviewId: Int,
        mediaId: Int,
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to delete a review"))

        val deleted = AnilistApi.deleteReview(reviewId = reviewId, token = token)
        if (deleted) {
            val cached = reviewsCache[mediaId]
            if (cached != null) {
                val filtered = cached.items.filter { it.id != reviewId }
                reviewsCache[mediaId] = cached.copy(items = filtered, total = (cached.total - 1).coerceAtLeast(0))
            }
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to delete review on AniList"))
        }
    }

    fun clearCache() {
        reviewsCache.clear()
    }
}
