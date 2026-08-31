package com.nuvio.app.features.anilist.recommendations

import com.nuvio.app.features.anilist.community.AnilistUserSummary
import kotlinx.serialization.Serializable

@Serializable
data class AnilistRecommendation(
    val id: Int,
    val rating: Int,
    val userRating: String? = null,
    val mediaId: Int,
    val title: String,
    val coverImage: String? = null,
    val averageScore: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val user: AnilistUserSummary? = null,
)

@Serializable
data class AnilistRecommendationPage(
    val recommendations: List<AnilistRecommendation> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
)
