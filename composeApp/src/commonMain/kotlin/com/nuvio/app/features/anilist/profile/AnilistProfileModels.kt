package com.nuvio.app.features.anilist.profile

import kotlinx.serialization.Serializable

@Serializable
data class AnilistFullUserProfile(
    val id: Int,
    val name: String,
    val about: String? = null,
    val avatarLarge: String? = null,
    val avatarMedium: String? = null,
    val bannerImage: String? = null,
    val donatorTier: Int = 0,
    val donatorBadge: String? = null,
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false,
    val animeCount: Int = 0,
    val episodesWatched: Int = 0,
    val minutesWatched: Long = 0L,
    val daysWatched: Double = 0.0,
    val meanScore: Double = 0.0,
    val favoriteAnime: List<AnilistProfileFavoriteAnime> = emptyList(),
    val favoriteCharacters: List<AnilistProfileFavoriteCharacter> = emptyList(),
)

@Serializable
data class AnilistProfileFavoriteAnime(
    val id: Int,
    val title: String,
    val coverImage: String? = null,
    val format: String? = null,
    val averageScore: Int? = null,
)

@Serializable
data class AnilistProfileFavoriteCharacter(
    val id: Int,
    val name: String,
    val image: String? = null,
)
