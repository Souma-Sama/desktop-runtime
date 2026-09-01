package com.nuvio.app.features.anilist.profile

import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnilistProfileRepository {
    private val profileCache = mutableMapOf<String, AnilistFullUserProfile>()

    suspend fun fetchUserProfile(
        userId: Int? = null,
        username: String? = null,
        forceRefresh: Boolean = false,
    ): Result<AnilistFullUserProfile> = withContext(Dispatchers.Default) {
        val cacheKey = userId?.toString() ?: username ?: "unknown"
        if (!forceRefresh) {
            profileCache[cacheKey]?.let { return@withContext Result.success(it) }
        }

        val token = AnilistAuthRepository.token.value
        val profile = AnilistApi.getUserProfile(
            userId = userId,
            username = username,
            token = token,
        ) ?: return@withContext Result.failure(Exception("User profile not found"))

        profileCache[cacheKey] = profile
        profileCache[profile.id.toString()] = profile
        profileCache[profile.name] = profile

        Result.success(profile)
    }

    suspend fun toggleFollow(userId: Int): Result<Boolean> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to follow users"))

        val isFollowing = AnilistApi.toggleFollowUser(userId = userId, token = token)

        // Update cached profile
        val cached = profileCache[userId.toString()]
        if (cached != null) {
            val updated = cached.copy(isFollowing = isFollowing)
            profileCache[userId.toString()] = updated
            profileCache[cached.name] = updated
        }

        Result.success(isFollowing)
    }

    private val statsCache = mutableMapOf<Int, AnilistUserStatistics>()
    private val activityCache = mutableMapOf<Int, List<AnilistUserActivityItem>>()

    suspend fun fetchUserStatistics(userId: Int, forceRefresh: Boolean = false): Result<AnilistUserStatistics> = withContext(Dispatchers.Default) {
        if (!forceRefresh) {
            statsCache[userId]?.let { return@withContext Result.success(it) }
        }
        val stats = AnilistApi.fetchUserStatistics(userId) ?: return@withContext Result.failure(Exception("Statistics not available"))
        statsCache[userId] = stats
        Result.success(stats)
    }

    suspend fun fetchUserActivity(userId: Int, page: Int = 1, forceRefresh: Boolean = false): Result<List<AnilistUserActivityItem>> = withContext(Dispatchers.Default) {
        if (!forceRefresh && page == 1) {
            activityCache[userId]?.let { return@withContext Result.success(it) }
        }
        val activities = AnilistApi.fetchUserActivityFeed(userId, page = page)
        if (page == 1) {
            activityCache[userId] = activities
        }
        Result.success(activities)
    }
}
