package com.nuvio.app.features.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AnilistMediaListStatus(val label: String) {
    @SerialName("CURRENT")
    CURRENT("Watching"),

    @SerialName("PLANNING")
    PLANNING("Plan to Watch"),

    @SerialName("COMPLETED")
    COMPLETED("Completed"),

    @SerialName("PAUSED")
    PAUSED("On Hold"),

    @SerialName("DROPPED")
    DROPPED("Dropped"),

    @SerialName("REPEATING")
    REPEATING("Rewatching");

    companion object {
        fun fromString(value: String?): AnilistMediaListStatus? =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}

@Serializable
data class AnilistTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
) {
    val displayTitle: String
        get() = english?.takeIf { it.isNotBlank() }
            ?: romaji?.takeIf { it.isNotBlank() }
            ?: native.orEmpty()
}

@Serializable
data class AnilistCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
) {
    val bestUrl: String?
        get() = extraLarge ?: large ?: medium
}

@Serializable
data class AnilistNextAiringEpisode(
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long,
)

@Serializable
data class AnilistStreamingEpisode(
    val title: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    val site: String? = null,
)

@Serializable
data class AnilistMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: AnilistTitle? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val coverImage: AnilistCoverImage? = null,
    val bannerImage: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val description: String? = null,
    val nextAiringEpisode: AnilistNextAiringEpisode? = null,
    val streamingEpisodes: List<AnilistStreamingEpisode> = emptyList(),
    val mediaListEntry: AnilistMediaListEntry? = null,
)

@Serializable
data class AnilistMediaListEntry(
    val id: Int = 0,
    val mediaId: Int = 0,
    val status: AnilistMediaListStatus? = null,
    val score: Double = 0.0,
    val progress: Int = 0,
    val repeat: Int = 0,
    val updatedAt: Long = 0L,
)

@Serializable
data class AnilistUser(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
)

data class AnilistTrackerState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isAnime: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: AnilistUser? = null,
    val media: AnilistMedia? = null,
    val entry: AnilistMediaListEntry? = null,
    val error: String? = null,
    val debugInfo: String? = null,
    val lastLookupTitle: String? = null,
    val lastLookupMediaId: String? = null,
    val resolvedStrategy: String? = null,
)
