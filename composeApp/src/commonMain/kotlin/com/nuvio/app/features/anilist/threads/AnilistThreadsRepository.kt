package com.nuvio.app.features.anilist.threads

import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnilistThreadsRepository {
    private val threadsCache = mutableMapOf<Int, AnilistThreadPage>()
    private val commentsCache = mutableMapOf<Int, List<AnilistThreadComment>>()

    suspend fun getMediaThreads(
        mediaId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
    ): Result<AnilistThreadPage> = withContext(Dispatchers.Default) {
        if (!forceRefresh && page == 1) {
            threadsCache[mediaId]?.let { return@withContext Result.success(it) }
        }

        val token = AnilistAuthRepository.token.value
        val threadPage = AnilistApi.getMediaThreads(
            mediaId = mediaId,
            page = page,
            token = token,
        )

        if (page == 1) {
            threadsCache[mediaId] = threadPage
        } else {
            val existing = threadsCache[mediaId]
            if (existing != null) {
                val combined = existing.threads + threadPage.threads.filter { t -> existing.threads.none { it.id == t.id } }
                threadsCache[mediaId] = threadPage.copy(threads = combined)
            }
        }

        Result.success(threadPage)
    }

    suspend fun getThreadComments(
        threadId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
    ): Result<List<AnilistThreadComment>> = withContext(Dispatchers.Default) {
        if (!forceRefresh && page == 1) {
            commentsCache[threadId]?.let { return@withContext Result.success(it) }
        }

        val token = AnilistAuthRepository.token.value
        val comments = AnilistApi.getThreadComments(
            threadId = threadId,
            page = page,
            token = token,
        )

        if (page == 1) {
            commentsCache[threadId] = comments
        } else {
            val existing = commentsCache[threadId] ?: emptyList()
            val combined = existing + comments.filter { c -> existing.none { it.id == c.id } }
            commentsCache[threadId] = combined
        }

        Result.success(comments)
    }

    suspend fun postComment(
        threadId: Int,
        comment: String,
    ): Result<AnilistThreadComment> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to comment"))

        val created = AnilistApi.saveThreadComment(
            threadId = threadId,
            comment = comment,
            token = token,
        ) ?: return@withContext Result.failure(Exception("Failed to post comment"))

        val existing = commentsCache[threadId] ?: emptyList()
        commentsCache[threadId] = existing + created

        Result.success(created)
    }

    suspend fun toggleLike(
        id: Int,
        type: String, // "THREAD", "THREAD_COMMENT"
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        val token = AnilistAuthRepository.token.value
            ?: return@withContext Result.failure(IllegalStateException("Must be logged in to like"))

        val success = AnilistApi.toggleLikeV2(id = id, type = type, token = token)
        Result.success(success)
    }
}
