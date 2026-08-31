package com.nuvio.app.features.anilist

import kotlinx.serialization.Serializable

object AnilistGenres {
    /**
     * Official AniList genres returned by AniList API GenreCollection.
     */
    val ALL_GENRES = listOf(
        "Action",
        "Adventure",
        "Comedy",
        "Drama",
        "Ecchi",
        "Fantasy",
        "Horror",
        "Mahou Shoujo",
        "Mecha",
        "Music",
        "Mystery",
        "Psychological",
        "Romance",
        "Sci-Fi",
        "Slice of Life",
        "Sports",
        "Supernatural",
        "Thriller",
    )

    val ADULT_GENRES = listOf("Hentai")

    fun getAvailableGenres(hideAdultContent: Boolean = true): List<String> {
        return if (hideAdultContent) ALL_GENRES else ALL_GENRES + ADULT_GENRES
    }

    /**
     * Popular thematic tags on AniList for advanced discovery.
     */
    val POPULAR_TAGS = listOf(
        "Isekai",
        "Shounen",
        "Seinen",
        "Shoujo",
        "Josei",
        "School",
        "Cyberpunk",
        "Post-Apocalyptic",
        "Military",
        "Super Power",
        "Time Travel",
        "Idol",
        "Martial Arts",
        "Vampire",
        "Survival",
        "Space",
        "Mythology",
        "Parody",
        "Detective",
    )
}

@Serializable
enum class AnilistSortOption(
    val label: String,
    val apiSortValue: String,
) {
    POPULARITY("Popularity", "POPULARITY_DESC"),
    SCORE("Highest Score", "SCORE_DESC"),
    TRENDING("Trending", "TRENDING_DESC"),
    NEWEST("Release Date (Newest)", "START_DATE_DESC"),
    OLDEST("Release Date (Oldest)", "START_DATE"),
    TITLE_AZ("Title (A-Z)", "TITLE_ROMAJI"),
    FAVORITES("Most Favorites", "FAVOURITES_DESC"),
    EPISODES("Episode Count", "EPISODES_DESC"),
    UPDATED("Recently Updated", "UPDATED_AT_DESC");

    companion object {
        fun fromLabelOrNull(label: String?): AnilistSortOption? {
            if (label.isNullOrBlank()) return null
            return entries.firstOrNull {
                it.label.equals(label, ignoreCase = true) ||
                    it.name.equals(label, ignoreCase = true) ||
                    it.apiSortValue.equals(label, ignoreCase = true)
            }
        }
    }
}
