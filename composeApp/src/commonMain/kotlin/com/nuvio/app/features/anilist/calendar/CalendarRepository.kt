package com.nuvio.app.features.anilist.calendar

import co.touchlab.kermit.Logger
import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

object CalendarRepository {
    private val log = Logger.withTag("CalendarRepository")
    private val mutex = Mutex()

    private var weeklyScheduleCache: Map<CalendarDay, List<CalendarMedia>>? = null
    private var weeklyScheduleCachedAtMs: Long = 0L

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var activeScheduleJob: kotlinx.coroutines.Job? = null

    fun setDay(day: CalendarDay) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    fun setGenreFilter(genre: String?) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
    }

    fun setSortOption(sort: CalendarSort) {
        _uiState.value = _uiState.value.copy(selectedSort = sort)
    }

    fun setOnMyList(onMyList: Boolean?) {
        _uiState.value = _uiState.value.copy(onMyList = onMyList)
    }

    fun clearCache() {
        scope.launch {
            mutex.withLock {
                weeklyScheduleCache = null
                weeklyScheduleCachedAtMs = 0L
            }
            loadWeeklySchedule(force = true)
        }
    }

    suspend fun loadWeeklySchedule(force: Boolean = false) {
        val now = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
        val cached = mutex.withLock {
            if (!force && weeklyScheduleCache != null && (now - weeklyScheduleCachedAtMs < 30 * 60 * 1000L)) {
                weeklyScheduleCache
            } else null
        }

        if (cached != null) {
            _uiState.value = _uiState.value.copy(scheduleItems = cached, isLoading = false, errorMessage = null)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        try {
            val schedule = fetchWeeklyScheduleFromApi()
            val hasItems = schedule.values.any { it.isNotEmpty() }
            if (!hasItems) {
                val existing = mutex.withLock { weeklyScheduleCache }
                if (existing != null && existing.values.any { it.isNotEmpty() }) {
                    _uiState.value = _uiState.value.copy(
                        scheduleItems = existing,
                        isLoading = false,
                        errorMessage = null,
                    )
                    return
                }
            }
            mutex.withLock {
                weeklyScheduleCache = schedule
                weeklyScheduleCachedAtMs = now
            }
            _uiState.value = _uiState.value.copy(
                scheduleItems = schedule,
                isLoading = false,
                errorMessage = null,
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to load weekly schedule" }
            val existing = mutex.withLock { weeklyScheduleCache }
            _uiState.value = _uiState.value.copy(
                scheduleItems = existing ?: emptyMap(),
                isLoading = false,
                errorMessage = if (existing == null || existing.values.all { it.isEmpty() }) {
                    e.message ?: "AniList service is temporarily unavailable. Please try again later."
                } else null,
            )
        }
    }

    private suspend fun fetchWeeklyScheduleFromApi(): Map<CalendarDay, List<CalendarMedia>> = withContext(Dispatchers.Default) {
        val nowMs = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
        val epochDay = (nowMs / 1000L) / 86400L
        val dayOfWeekIndex = (((epochDay + 3) % 7 + 7) % 7).toInt() // 0 = Monday, 6 = Sunday

        val mondayStartSec = (epochDay - dayOfWeekIndex) * 86400L
        val sundayEndSec = mondayStartSec + (7 * 86400L) - 1

        val query = """
            query AiringSchedule(${'$'}airingAt_greater: Int, ${'$'}airingAt_lesser: Int) {
              Page(page: 1, perPage: 50) {
                airingSchedules(airingAt_greater: ${'$'}airingAt_greater, airingAt_lesser: ${'$'}airingAt_lesser, sort: TIME) {
                  id
                  episode
                  airingAt
                  timeUntilAiring
                  media {
                    id
                    isAdult
                    popularity
                    title {
                      english
                      romaji
                      native
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    format
                    episodes
                    duration
                    status
                    genres
                    averageScore
                    source
                    mediaListEntry {
                      id
                      status
                      score
                      progress
                    }
                    studios(isMain: true) {
                      nodes {
                        name
                      }
                    }
                    description(asHtml: false)
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("airingAt_greater", mondayStartSec)
            put("airingAt_lesser", sundayEndSec)
        }

        val json = AnilistApi.executeGraphQL(query, variables)
            ?: throw IllegalStateException("AniList service is temporarily unavailable. Please try again later.")
        val pageObj = json["data"].asJsonObjectOrNull()?.get("Page").asJsonObjectOrNull()
            ?: throw IllegalStateException("Unable to parse airing schedule from AniList.")
        val schedules = pageObj["airingSchedules"].asJsonArrayOrNull()
            ?: throw IllegalStateException("Unable to parse airing schedule items.")

        val hideAdult = AnilistPreferencesRepository.snapshot().hideAdultContent
        val preferredLang = AnilistPreferencesRepository.snapshot().preferredTitleLanguage.name
        val resultMap = mutableMapOf<CalendarDay, MutableList<CalendarMedia>>()
        CalendarDay.entries.forEach { resultMap[it] = mutableListOf() }

        schedules.forEach { scheduleElem ->
            val sObj = scheduleElem.asJsonObjectOrNull() ?: return@forEach
            val episode = sObj["episode"].asIntOrNull()
            val airingAt = sObj["airingAt"].asLongOrNull() ?: return@forEach
            val timeUntil = sObj["timeUntilAiring"].asLongOrNull()
            val mediaObj = sObj["media"].asJsonObjectOrNull() ?: return@forEach

            val media = parseCalendarMedia(mediaObj, preferredLang, explicitAiringAt = airingAt, explicitNextEp = episode, explicitTimeUntil = timeUntil)
            if (media != null && (!hideAdult || !media.isAdult)) {
                val day = CalendarDay.fromEpochSeconds(airingAt)
                resultMap[day]?.add(media)
            }
        }

        resultMap.forEach { (_, list) ->
            list.sortBy { it.airingAt ?: Long.MAX_VALUE }
        }

        resultMap
    }

    private fun parseCalendarMedia(
        obj: JsonObject,
        preferredLang: String,
        explicitAiringAt: Long? = null,
        explicitNextEp: Int? = null,
        explicitTimeUntil: Long? = null,
    ): CalendarMedia? {
        val id = obj["id"].asIntOrNull() ?: return null
        val isAdult = obj["isAdult"].asBooleanOrNull() ?: false
        val popularity = obj["popularity"].asIntOrNull()
        val titleObj = obj["title"].asJsonObjectOrNull()
        val english = titleObj?.get("english").asStringOrNull()
        val romaji = titleObj?.get("romaji").asStringOrNull()
        val native = titleObj?.get("native").asStringOrNull()

        val title = when (preferredLang.uppercase()) {
            "ENGLISH" -> english?.takeIf { it.isNotBlank() } ?: romaji ?: native ?: "Anime #$id"
            "NATIVE", "JAPANESE" -> native?.takeIf { it.isNotBlank() } ?: romaji ?: english ?: "Anime #$id"
            else -> romaji?.takeIf { it.isNotBlank() } ?: english ?: native ?: "Anime #$id"
        }

        val covObj = obj["coverImage"].asJsonObjectOrNull()
        val poster = covObj?.get("extraLarge").asStringOrNull() ?: covObj?.get("large").asStringOrNull()
        val banner = obj["bannerImage"].asStringOrNull()
        val format = obj["format"].asStringOrNull()
        val episodes = obj["episodes"].asIntOrNull()
        val status = obj["status"].asStringOrNull()
        val score = obj["averageScore"].asDoubleOrNull()?.let { (it / 10.0 * 10).toInt() / 10.0 }
        val source = obj["source"].asStringOrNull()?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
        val desc = obj["description"].asStringOrNull()

        val studio = obj["studios"].asJsonObjectOrNull()
            ?.get("nodes").asJsonArrayOrNull()
            ?.firstOrNull()?.asJsonObjectOrNull()
            ?.get("name").asStringOrNull()

        val genres = obj["genres"].asJsonArrayOrNull()?.mapNotNull { it.asStringOrNull() }.orEmpty()

        val nextAirObj = obj["nextAiringEpisode"].asJsonObjectOrNull()
        val airingAt = explicitAiringAt ?: nextAirObj?.get("airingAt").asLongOrNull()
        val timeUntil = explicitTimeUntil ?: nextAirObj?.get("timeUntilAiring").asLongOrNull()
        val nextEp = explicitNextEp ?: nextAirObj?.get("episode").asIntOrNull()

        val sDateObj = obj["startDate"].asJsonObjectOrNull()
        val sYear = sDateObj?.get("year").asIntOrNull()
        val sMonth = sDateObj?.get("month").asIntOrNull()
        val sDay = sDateObj?.get("day").asIntOrNull()
        val startDateStr = if (sYear != null && sMonth != null && sDay != null) {
            "${sYear.toString().padStart(4, '0')}-${sMonth.toString().padStart(2, '0')}-${sDay.toString().padStart(2, '0')}"
        } else null

        val listEntryObj = obj["mediaListEntry"].asJsonObjectOrNull()
        val listStatus = listEntryObj?.get("status").asStringOrNull()

        return CalendarMedia(
            id = id,
            title = title,
            poster = poster,
            banner = banner,
            format = format,
            episodes = episodes,
            status = status,
            genres = genres,
            score = score,
            popularity = popularity,
            studio = studio,
            source = source,
            airingAt = airingAt,
            timeUntilAiring = timeUntil,
            nextEpisode = nextEp,
            startDate = startDateStr,
            description = desc,
            isAdult = isAdult,
            listStatus = listStatus,
        )
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = if (this is JsonObject) this else null
    private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = if (this is JsonArray) this else null
    private fun JsonElement?.asStringOrNull(): String? = if (this is JsonPrimitive && this !is JsonNull) this.contentOrNull else null
    private fun JsonElement?.asIntOrNull(): Int? = if (this is JsonPrimitive) this.intOrNull else null
    private fun JsonElement?.asLongOrNull(): Long? = if (this is JsonPrimitive) this.longOrNull else null
    private fun JsonElement?.asDoubleOrNull(): Double? = if (this is JsonPrimitive) this.doubleOrNull else null
    private fun JsonElement?.asBooleanOrNull(): Boolean? = if (this is JsonPrimitive) this.booleanOrNull else null
}

typealias AniChartRepository = CalendarRepository
