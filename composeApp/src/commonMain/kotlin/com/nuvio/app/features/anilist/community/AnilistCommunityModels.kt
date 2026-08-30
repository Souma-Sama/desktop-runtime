package com.nuvio.app.features.anilist.community

import kotlinx.serialization.Serializable

@Serializable
data class AnilistUserSummary(
    val id: Int = 0,
    val name: String = "",
    val avatarMedium: String? = null,
    val avatarLarge: String? = null,
    val bannerImage: String? = null,
    val donatorBadge: String? = null,
)

@Serializable
data class AnilistReview(
    val id: Int,
    val userId: Int = 0,
    val mediaId: Int = 0,
    val summary: String = "",
    val body: String = "",
    val rating: Int = 0,
    val ratingAmount: Int = 0,
    val userRating: String? = null, // "UP_VOTE", "DOWN_VOTE", "NO_RATING"
    val score: Int = 0,
    val siteUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val user: AnilistUserSummary? = null,
) {
    val isHelpfulPositive: Boolean
        get() = rating > 0

    val helpfulPercentage: Int
        get() = if (ratingAmount > 0) ((rating.toDouble() / ratingAmount) * 100).toInt().coerceIn(0, 100) else 0
}

@Serializable
data class AnilistReviewPage(
    val items: List<AnilistReview> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val total: Int = 0,
)

enum class AnilistReviewVote {
    NO_RATING,
    UP_VOTE,
    DOWN_VOTE,
}
