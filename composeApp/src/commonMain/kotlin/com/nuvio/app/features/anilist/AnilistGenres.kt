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
     * Official AniList tags categorized for structured multi-filtering.
     */
    val POPULAR_TAGS = listOf(
        "Isekai",
        "Reincarnation",
        "Time Travel",
        "Cyberpunk",
        "Post-Apocalyptic",
        "Dystopian",
        "Steampunk",
        "Super Power",
        "Martial Arts",
        "Swordplay",
        "Magic",
        "Mythology",
        "Demons",
        "Vampire",
        "Zombies",
        "Survival",
        "Battle Royale",
        "Death Game",
        "Space",
        "Space Opera",
        "Military",
        "War",
        "Politics",
        "Conspiracy",
        "Crime",
        "Detective",
        "Police",
        "Mafia",
        "Assassins",
        "Spy",
        "Idol",
        "Band",
        "Video Games",
        "Virtual Reality",
        "Otaku Culture",
        "Gourmet",
        "Cooking",
        "Iyashikei",
        "Cute Girls Doing Cute Things",
        "School",
        "Workplace",
        "Historical",
        "Feudal Japan",
        "Medieval",
        "Coming of Age",
        "Tragedy",
        "Revenge",
        "Dark Fantasy",
        "High Fantasy",
        "Urban Fantasy",
        "Cultivation",
        "Wuxia",
        "Shounen",
        "Seinen",
        "Shoujo",
        "Josei",
        "Monster Girls",
        "Kemonomimi",
    )

    val FORMAT_OPTIONS = listOf(
        "TV" to "TV Series",
        "TV_SHORT" to "TV Short",
        "MOVIE" to "Movie",
        "OVA" to "OVA",
        "ONA" to "ONA",
        "SPECIAL" to "Special",
    )

    val STATUS_OPTIONS = listOf(
        "RELEASING" to "Currently Airing",
        "FINISHED" to "Finished",
        "NOT_YET_RELEASED" to "Upcoming",
        "CANCELLED" to "Cancelled",
        "HIATUS" to "On Hiatus",
    )

    val SEASON_OPTIONS = listOf(
        "WINTER" to "Winter",
        "SPRING" to "Spring",
        "SUMMER" to "Summer",
        "FALL" to "Fall",
    )

    val COUNTRY_OPTIONS = listOf(
        "JP" to "Japan (Anime)",
        "CN" to "China (Donghua)",
        "KR" to "South Korea",
        "TW" to "Taiwan",
    )

    val SOURCE_OPTIONS = listOf(
        "ORIGINAL" to "Original",
        "MANGA" to "Manga",
        "LIGHT_NOVEL" to "Light Novel",
        "VISUAL_NOVEL" to "Visual Novel",
        "VIDEO_GAME" to "Video Game",
        "WEB_NOVEL" to "Web Novel / Manhwa",
        "NOVEL" to "Novel",
        "OTHER" to "Other",
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
