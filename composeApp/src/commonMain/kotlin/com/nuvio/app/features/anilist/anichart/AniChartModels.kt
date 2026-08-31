package com.nuvio.app.features.anilist.anichart

import kotlinx.serialization.Serializable

@Serializable
enum class AniChartSeason(val apiName: String, val label: String) {
    WINTER("WINTER", "Winter"),
    SPRING("SPRING", "Spring"),
    SUMMER("SUMMER", "Summer"),
    FALL("FALL", "Fall"),
    ;

    companion object {
        fun current(): AniChartSeason {
            // Jan, Feb, Mar -> WINTER; Apr, May, Jun -> SPRING; Jul, Aug, Sep -> SUMMER; Oct, Nov, Dec -> FALL
            val nowMs = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
            val isoDate = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.localIsoDateAtEpochMs(nowMs)
            val month = isoDate?.split('-')?.getOrNull(1)?.toIntOrNull() ?: 1
            return when (month) {
                1, 2, 3 -> WINTER
                4, 5, 6 -> SPRING
                7, 8, 9 -> SUMMER
                else -> FALL
            }
        }

        fun currentYear(): Int {
            val nowMs = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
            val isoDate = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.localIsoDateAtEpochMs(nowMs)
            return isoDate?.split('-')?.getOrNull(0)?.toIntOrNull() ?: 2025
        }
    }
}

enum class AniChartMode(val label: String) {
    SEASONAL("Seasonal"),
    SCHEDULE("Weekly Schedule"),
}

enum class AniChartDay(val label: String, val shortLabel: String, val dayIndex: Int) {
    MONDAY("Monday", "Mon", 1),
    TUESDAY("Tuesday", "Tue", 2),
    WEDNESDAY("Wednesday", "Wed", 3),
    THURSDAY("Thursday", "Thu", 4),
    FRIDAY("Friday", "Fri", 5),
    SATURDAY("Saturday", "Sat", 6),
    SUNDAY("Sunday", "Sun", 7),
    ;

    companion object {
        fun fromEpochSeconds(epochSec: Long): AniChartDay {
            val epochDay = (epochSec / 86400L)
            // 1970-01-01 was Thursday (day 4). (epochDay + 3) % 7 gives 0 for Monday, 6 for Sunday
            val dayOfWeek = (((epochDay + 3) % 7 + 7) % 7).toInt() + 1
            return entries.firstOrNull { it.dayIndex == dayOfWeek } ?: MONDAY
        }

        fun today(): AniChartDay {
            val nowMs = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
            return fromEpochSeconds(nowMs / 1000L)
        }
    }
}

enum class AniChartFormatFilter(val label: String) {
    ALL("All"),
    TV("TV"),
    TV_SHORT("Shorts"),
    MOVIE("Movies"),
    OVA_ONA("OVA / ONA"),
}

enum class AniChartSort(val label: String) {
    POPULARITY("Popularity"),
    SCORE("Highest Score"),
    TITLE("Title (A-Z)"),
    EPISODES("Episodes"),
    AIRING_TIME("Airing Time"),
}

@Serializable
data class AniChartMedia(
    val id: Int,
    val title: String,
    val poster: String?,
    val banner: String?,
    val format: String?,
    val episodes: Int?,
    val status: String?,
    val genres: List<String> = emptyList(),
    val score: Double? = null,
    val popularity: Int? = null,
    val studio: String? = null,
    val source: String? = null,
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null,
    val nextEpisode: Int? = null,
    val startDate: String? = null,
    val description: String? = null,
    val isContinuing: Boolean = false,
    val isAdult: Boolean = false,
)

data class AniChartUiState(
    val mode: AniChartMode = AniChartMode.SEASONAL,
    val selectedSeason: AniChartSeason = AniChartSeason.current(),
    val selectedYear: Int = AniChartSeason.currentYear(),
    val selectedDay: AniChartDay = AniChartDay.today(),
    val selectedFormat: AniChartFormatFilter = AniChartFormatFilter.ALL,
    val selectedGenre: String? = null,
    val selectedSort: AniChartSort = AniChartSort.POPULARITY,
    val seasonalItems: List<AniChartMedia> = emptyList(),
    val scheduleItems: Map<AniChartDay, List<AniChartMedia>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
