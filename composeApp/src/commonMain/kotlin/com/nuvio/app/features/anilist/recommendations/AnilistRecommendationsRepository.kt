package com.nuvio.app.features.anilist.recommendations

import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnilistRecommendationsRepository {
    private val recommendationsCache = mutableMapOf<Int, AnilistRecommendationPage>()

    suspend fun getMediaRecommendations(
        mediaId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
    ): Result<AnilistRecommendationPage> = withContext(Dispatchers.Default) {
        if (!forceRefresh && page == 1) {
            recommendationsCache[mediaId]?.let { return@withContext Result.success(it) }
        }

        val token = AnilistAuthRepository.token.value
        val recPage = AnilistApi.getMediaRecommendations(
            mediaId = mediaId,
            page = page,
            token = token,
        )

        if (page == 1) {
            recommendationsCache[mediaId] = recPage
        } else {
            val existing = recommendationsCache[mediaId]
            if (existing != null) {
                val combined = existing.recommendations + recPage.recommendations.filter { r -> existing.recommendations.none { it.id == r.id } }
                recommendationsCache[mediaId] = recPage.copy(recommendations = combined)
            }
        }

        Result.success(recPage)
    }

    suspend fun voteRecommendation(
        mediaId: Int,
        mediaRecommendationId: Int,
        rating: String, // "RATE_UP", "RATE_DOWN", "NO_RATING"
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to vote"))

        val success = AnilistApi.saveRecommendationVote(
            mediaId = mediaId,
            mediaRecommendationId = mediaRecommendationId,
            rating = rating,
            token = token,
        )

        Result.success(success)
    }
}
