package com.nuvio.app.features.anilist.threads

import com.nuvio.app.features.anilist.community.AnilistUserSummary
import kotlinx.serialization.Serializable

@Serializable
data class AnilistThread(
    val id: Int,
    val title: String,
    val body: String = "",
    val replyCount: Int = 0,
    val viewCount: Int = 0,
    val isSticky: Boolean = false,
    val isLocked: Boolean = false,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val createdAt: Long = 0L,
    val user: AnilistUserSummary? = null,
)

@Serializable
data class AnilistThreadComment(
    val id: Int,
    val threadId: Int,
    val comment: String,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val createdAt: Long = 0L,
    val user: AnilistUserSummary? = null,
)

@Serializable
data class AnilistThreadPage(
    val threads: List<AnilistThread> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
)
