package com.nuvio.app.features.anilist.calendar

import kotlinx.serialization.Serializable

@Serializable
enum class AnimeSeason(val apiName: String, val label: String) {
    WINTER("WINTER", "Winter"),
    SPRING("SPRING", "Spring"),
    SUMMER("SUMMER", "Summer"),
    FALL("FALL", "Fall"),
    ;

    companion object {
        fun current(): AnimeSeason {
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

typealias AniChartSeason = AnimeSeason

enum class CalendarDay(val label: String, val shortLabel: String, val dayIndex: Int) {
    MONDAY("Monday", "Mon", 1),
    TUESDAY("Tuesday", "Tue", 2),
    WEDNESDAY("Wednesday", "Wed", 3),
    THURSDAY("Thursday", "Thu", 4),
    FRIDAY("Friday", "Fri", 5),
    SATURDAY("Saturday", "Sat", 6),
    SUNDAY("Sunday", "Sun", 7),
    ;

    companion object {
        fun fromEpochSeconds(epochSec: Long): CalendarDay {
            val epochDay = (epochSec / 86400L)
            val dayOfWeek = (((epochDay + 3) % 7 + 7) % 7).toInt() + 1
            return entries.firstOrNull { it.dayIndex == dayOfWeek } ?: MONDAY
        }

        fun today(): CalendarDay {
            val nowMs = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
            return fromEpochSeconds(nowMs / 1000L)
        }
    }
}

enum class CalendarSort(val label: String) {
    AIRING_TIME("Airing Time"),
    POPULARITY("Popularity"),
    SCORE("Highest Score"),
    TITLE("Title (A-Z)"),
    EPISODES("Episodes"),
}

@Serializable
data class CalendarMedia(
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
    val listStatus: String? = null,
) {
    val onMyList: Boolean get() = listStatus != null
}

data class CalendarUiState(
    val selectedDay: CalendarDay = CalendarDay.today(),
    val selectedGenre: String? = null,
    val selectedSort: CalendarSort = CalendarSort.AIRING_TIME,
    val onMyList: Boolean? = null,
    val scheduleItems: Map<CalendarDay, List<CalendarMedia>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
