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
    fun getDisplayTitle(preference: AnilistTitleLanguage = AnilistPreferencesRepository.snapshot().preferredTitleLanguage): String {
        return when (preference) {
            AnilistTitleLanguage.ROMAJI -> romaji?.takeIf { it.isNotBlank() } ?: english?.takeIf { it.isNotBlank() } ?: native.orEmpty()
            AnilistTitleLanguage.ENGLISH -> english?.takeIf { it.isNotBlank() } ?: romaji?.takeIf { it.isNotBlank() } ?: native.orEmpty()
            AnilistTitleLanguage.NATIVE -> native?.takeIf { it.isNotBlank() } ?: romaji?.takeIf { it.isNotBlank() } ?: english.orEmpty()
        }
    }

    val displayTitle: String
        get() = getDisplayTitle()
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
data class AnilistCharacterVoiceActor(
    val id: Int? = null,
    val name: String? = null,
    val image: String? = null,
    val language: String? = null,
)

@Serializable
data class AnilistCharacter(
    val id: Int? = null,
    val name: String? = null,
    val role: String? = null,
    val image: String? = null,
    val voiceActor: AnilistCharacterVoiceActor? = null,
    val voiceActors: List<AnilistCharacterVoiceActor> = emptyList(),
) {
    val japaneseVoiceActor: AnilistCharacterVoiceActor?
        get() = voiceActors.firstOrNull { it.language.equals("Japanese", ignoreCase = true) } ?: voiceActor

    val englishVoiceActor: AnilistCharacterVoiceActor?
        get() = voiceActors.firstOrNull { it.language.equals("English", ignoreCase = true) }
}

@Serializable
data class AnilistStudio(
    val id: Int? = null,
    val name: String? = null,
    val isAnimationStudio: Boolean = false,
)

@Serializable
data class AnilistRecommendation(
    val id: Int,
    val title: AnilistTitle? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val coverImage: AnilistCoverImage? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
)

@Serializable
data class AnilistTrailerInfo(
    val id: String? = null,
    val site: String? = null,
)

@Serializable
data class AnilistStaff(
    val id: Int? = null,
    val name: String? = null,
    val role: String? = null,
    val image: String? = null,
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
    val characters: List<AnilistCharacter> = emptyList(),
    val studios: List<AnilistStudio> = emptyList(),
    val recommendations: List<AnilistRecommendation> = emptyList(),
    val trailer: AnilistTrailerInfo? = null,
    val staff: List<AnilistStaff> = emptyList(),
    val startDateYear: Int? = null,
    val startDateMonth: Int? = null,
    val startDateDay: Int? = null,
    val endDateYear: Int? = null,
    val airingSchedule: Map<Int, Long> = emptyMap(),
    val mediaListEntry: AnilistMediaListEntry? = null,
    val relations: List<AnilistRelation> = emptyList(),
    val isFullDetails: Boolean = false,
)

@Serializable
data class AnilistRelation(
    val relationType: String? = null,
    val id: Int,
    val title: AnilistTitle? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val coverImage: AnilistCoverImage? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val relations: List<AnilistRelation> = emptyList(),
)

@Serializable
data class AnilistFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
) {
    val isSet: Boolean
        get() = year != null || month != null || day != null

    fun formatted(): String {
        if (!isSet) return "Not set"
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthStr = if (month != null && month in 1..12) months[month - 1] else null
        return listOfNotNull(
            day?.toString(),
            monthStr,
            year?.toString(),
        ).joinToString(" ")
    }
}

@Serializable
data class AnilistMediaListEntry(
    val id: Int = 0,
    val mediaId: Int = 0,
    val status: AnilistMediaListStatus? = null,
    val score: Double = 0.0,
    val progress: Int = 0,
    val repeat: Int = 0,
    val private: Boolean = false,
    val hiddenFromStatusLists: Boolean = false,
    val notes: String? = null,
    val startedAt: AnilistFuzzyDate? = null,
    val completedAt: AnilistFuzzyDate? = null,
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

@Serializable
data class AnilistLibraryItem(
    val id: Int,
    val title: String,
    val posterUrl: String? = null,
    val progress: Int = 0,
    val totalEpisodes: Int? = null,
    val score: Double? = null,
    val airingStatus: String? = null,
    val status: String = "",
    val updatedAt: Long = 0L,
    val entryId: Int = 0,
    val format: String? = null,
    val imdbId: String? = null,
)

@Serializable
data class AnilistLibraryUiState(
    val watching: List<AnilistLibraryItem> = emptyList(),
    val completed: List<AnilistLibraryItem> = emptyList(),
    val planning: List<AnilistLibraryItem> = emptyList(),
    val paused: List<AnilistLibraryItem> = emptyList(),
    val dropped: List<AnilistLibraryItem> = emptyList(),
    val rewatching: List<AnilistLibraryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val errorMessage: String? = null,
)

@Serializable
enum class AnilistSortBy(val label: String) {
    LAST_UPDATED("Last Updated"),
    SCORE("Score"),
    TITLE("Title"),
    RELEASE_DATE("Release Date"),
}

@Serializable
data class AnilistLibraryMenuPrefsState(
    val sortBy: AnilistSortBy = AnilistSortBy.LAST_UPDATED,
    val sortAscending: Boolean = false,
    val openByCatalogUrl: String? = null,
)

@Serializable
data class AniZipResponse(
    val titles: Map<String, String>? = null,
    val episodes: Map<String, AniZipEpisode>? = null,
    val mappings: AniZipMappings? = null,
)

@Serializable
data class AniZipEpisode(
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val absoluteEpisodeNumber: Int? = null,
    val airDate: String? = null,
    val airDateUtc: String? = null,
)

@Serializable
data class AniZipMappings(
    @SerialName("anilist_id") val anilistId: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
)
