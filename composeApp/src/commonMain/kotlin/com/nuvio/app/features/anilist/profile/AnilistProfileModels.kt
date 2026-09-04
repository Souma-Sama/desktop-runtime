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
    val favoriteManga: List<AnilistProfileFavoriteAnime> = emptyList(),
    val favoriteCharacters: List<AnilistProfileFavoriteCharacter> = emptyList(),
    val favoriteStaff: List<AnilistProfileFavoriteCharacter> = emptyList(),
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

@Serializable
data class AnilistScoreDistributionItem(
    val score: Int,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
    val chaptersRead: Int = 0,
)

@Serializable
data class AnilistFormatStatItem(
    val format: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
    val chaptersRead: Int = 0,
)

@Serializable
data class AnilistStatusStatItem(
    val status: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
    val chaptersRead: Int = 0,
)

@Serializable
data class AnilistReleaseYearStatItem(
    val releaseYear: Int,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistGenreStatItem(
    val genre: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
    val chaptersRead: Int = 0,
)

@Serializable
data class AnilistTagStatItem(
    val id: Int = 0,
    val name: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
    val chaptersRead: Int = 0,
)

@Serializable
data class AnilistStaffStatItem(
    val id: Int = 0,
    val name: String,
    val image: String? = null,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistStudioStatItem(
    val id: Int = 0,
    val name: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistVoiceActorStatItem(
    val id: Int = 0,
    val name: String,
    val image: String? = null,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistCountryStatItem(
    val country: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistLengthStatItem(
    val length: String,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistStartYearStatItem(
    val startYear: Int,
    val count: Int,
    val meanScore: Double = 0.0,
    val minutesWatched: Long = 0L,
)

@Serializable
data class AnilistMediumStatistics(
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val standardDeviation: Double = 0.0,
    val minutesWatched: Long = 0L,
    val daysWatched: Double = 0.0,
    val episodesWatched: Int = 0,
    val chaptersRead: Int = 0,
    val volumesRead: Int = 0,
    val daysPlanned: Double = 0.0,
    val scores: List<AnilistScoreDistributionItem> = emptyList(),
    val formats: List<AnilistFormatStatItem> = emptyList(),
    val statuses: List<AnilistStatusStatItem> = emptyList(),
    val releaseYears: List<AnilistReleaseYearStatItem> = emptyList(),
    val startYears: List<AnilistStartYearStatItem> = emptyList(),
    val countries: List<AnilistCountryStatItem> = emptyList(),
    val lengths: List<AnilistLengthStatItem> = emptyList(),
    val genres: List<AnilistGenreStatItem> = emptyList(),
    val tags: List<AnilistTagStatItem> = emptyList(),
    val staff: List<AnilistStaffStatItem> = emptyList(),
    val voiceActors: List<AnilistVoiceActorStatItem> = emptyList(),
    val studios: List<AnilistStudioStatItem> = emptyList(),
)

@Serializable
data class AnilistUserStatistics(
    val anime: AnilistMediumStatistics = AnilistMediumStatistics(),
    val manga: AnilistMediumStatistics = AnilistMediumStatistics(),
)

@Serializable
data class AnilistUserActivityItem(
    val id: Int,
    val status: String,
    val progress: String? = null,
    val createdAt: Long = 0L,
    val mediaId: Int,
    val mediaTitle: String,
    val coverImage: String? = null,
    val format: String? = null,
)

@Serializable
data class AnilistSocialUserItem(
    val id: Int,
    val name: String,
    val avatar: String? = null,
    val donatorBadge: String? = null,
    val isFollowing: Boolean = false,
)
