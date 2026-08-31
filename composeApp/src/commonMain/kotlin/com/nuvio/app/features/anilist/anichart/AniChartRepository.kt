package com.nuvio.app.features.anilist.anichart

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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

object AniChartRepository {
    private val log = Logger.withTag("AniChartRepository")
    private val mutex = Mutex()

    private val seasonalCache = mutableMapOf<String, List<AniChartMedia>>()
    private var weeklyScheduleCache: Map<AniChartDay, List<AniChartMedia>>? = null
    private var weeklyScheduleCachedAtMs: Long = 0L

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(AniChartUiState())
    val uiState: StateFlow<AniChartUiState> = _uiState.asStateFlow()

    fun setMode(mode: AniChartMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
        if (mode == AniChartMode.SEASONAL && _uiState.value.seasonalItems.isEmpty()) {
            scope.launch { loadSeasonal(_uiState.value.selectedSeason, _uiState.value.selectedYear) }
        } else if (mode == AniChartMode.SCHEDULE && _uiState.value.scheduleItems.isEmpty()) {
            scope.launch { loadWeeklySchedule() }
        }
    }

    fun setSeason(season: AniChartSeason, year: Int) {
        val current = _uiState.value
        if (current.selectedSeason == season && current.selectedYear == year && current.seasonalItems.isNotEmpty()) return
        scope.launch {
            loadSeasonal(season, year)
        }
    }

    fun setDay(day: AniChartDay) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    fun setFormatFilter(format: AniChartFormatFilter) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun setGenreFilter(genre: String?) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
    }

    fun setSortOption(sort: AniChartSort) {
        _uiState.value = _uiState.value.copy(selectedSort = sort)
    }

    fun clearCache() {
        scope.launch {
            mutex.withLock {
                seasonalCache.clear()
                weeklyScheduleCache = null
                weeklyScheduleCachedAtMs = 0L
            }
            if (_uiState.value.mode == AniChartMode.SEASONAL) {
                loadSeasonal(_uiState.value.selectedSeason, _uiState.value.selectedYear, force = true)
            } else {
                loadWeeklySchedule(force = true)
            }
        }
    }

    suspend fun loadSeasonal(season: AniChartSeason, year: Int, force: Boolean = false) {
        val cacheKey = "${season.apiName}_$year"
        if (!force) {
            val cached = mutex.withLock { seasonalCache[cacheKey] }
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    seasonalItems = cached,
                    selectedSeason = season,
                    selectedYear = year,
                    isLoading = false,
                    errorMessage = null,
                )
                return
            }
        }

        _uiState.value = _uiState.value.copy(
            seasonalItems = emptyList(),
            selectedSeason = season,
            selectedYear = year,
            isLoading = true,
            errorMessage = null,
        )

        val items = fetchSeasonalFromApi(season, year)
        if (items.isNotEmpty()) {
            mutex.withLock { seasonalCache[cacheKey] = items }
        }

        _uiState.value = _uiState.value.copy(
            seasonalItems = items,
            isLoading = false,
            errorMessage = if (items.isEmpty()) "No anime found for ${season.label} $year" else null,
        )
    }

    suspend fun loadWeeklySchedule(force: Boolean = false) {
        val now = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
        if (!force && weeklyScheduleCache != null && (now - weeklyScheduleCachedAtMs < 30 * 60 * 1000L)) {
            _uiState.value = _uiState.value.copy(
                scheduleItems = weeklyScheduleCache.orEmpty(),
                isLoading = false,
                errorMessage = null,
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        val scheduleMap = fetchWeeklyScheduleFromApi()
        weeklyScheduleCache = scheduleMap
        weeklyScheduleCachedAtMs = now

        _uiState.value = _uiState.value.copy(
            scheduleItems = scheduleMap,
            isLoading = false,
            errorMessage = if (scheduleMap.isEmpty()) "Failed to load schedule" else null,
        )
    }

    private suspend fun fetchSeasonalFromApi(season: AniChartSeason, year: Int): List<AniChartMedia> = withContext(Dispatchers.Default) {
        val query = """
            query (${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}page: Int, ${'$'}isAdult: Boolean) {
              Page(page: ${'$'}page, perPage: 50) {
                media(season: ${'$'}season, seasonYear: ${'$'}seasonYear, type: ANIME, sort: [POPULARITY_DESC], isAdult: ${'$'}isAdult) {
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
                  nextAiringEpisode {
                    episode
                    airingAt
                    timeUntilAiring
                  }
                  startDate {
                    year
                    month
                    day
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
        """.trimIndent()

        val hideAdult = AnilistPreferencesRepository.snapshot().hideAdultContent
        val variables = buildJsonObject {
            put("season", season.apiName)
            put("seasonYear", year)
            put("page", 1)
            if (hideAdult) put("isAdult", false)
        }

        val json = AnilistApi.executeGraphQL(query, variables) ?: return@withContext emptyList()
        val mediaArray = json["data"].asJsonObjectOrNull()
            ?.get("Page").asJsonObjectOrNull()
            ?.get("media").asJsonArrayOrNull()
            ?: return@withContext emptyList()

        val preferredLang = AnilistPreferencesRepository.snapshot().preferredTitleLanguage.name
        mediaArray.mapNotNull {
            it.asJsonObjectOrNull()?.let { obj -> parseAniChartMedia(obj, preferredLang, currentSeason = season, currentYear = year) }
        }.filter { if (hideAdult) !it.isAdult else true }
    }

    private suspend fun fetchWeeklyScheduleFromApi(): Map<AniChartDay, List<AniChartMedia>> = withContext(Dispatchers.Default) {
        val nowMs = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.nowEpochMs()
        val nowSec = (nowMs / 1000L)
        val epochDay = nowSec / 86400L
        val dayOfWeekIndex = (((epochDay + 3) % 7 + 7) % 7).toInt() // 0 = Mon, 6 = Sun
        val mondayStartSec = (epochDay - dayOfWeekIndex) * 86400L
        val sundayEndSec = mondayStartSec + 7 * 86400L

        val query = """
            query (${'$'}airingAt_greater: Int, ${'$'}airingAt_lesser: Int) {
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

        val json = AnilistApi.executeGraphQL(query, variables) ?: return@withContext emptyMap()
        val schedules = json["data"].asJsonObjectOrNull()
            ?.get("Page").asJsonObjectOrNull()
            ?.get("airingSchedules").asJsonArrayOrNull()
            ?: return@withContext emptyMap()

        val hideAdult = AnilistPreferencesRepository.snapshot().hideAdultContent
        val preferredLang = AnilistPreferencesRepository.snapshot().preferredTitleLanguage.name
        val resultMap = mutableMapOf<AniChartDay, MutableList<AniChartMedia>>()
        AniChartDay.entries.forEach { resultMap[it] = mutableListOf() }

        schedules.forEach { scheduleElem ->
            val sObj = scheduleElem.asJsonObjectOrNull() ?: return@forEach
            val episode = sObj["episode"].asIntOrNull()
            val airingAt = sObj["airingAt"].asLongOrNull() ?: return@forEach
            val timeUntil = sObj["timeUntilAiring"].asLongOrNull()
            val mediaObj = sObj["media"].asJsonObjectOrNull() ?: return@forEach

            val media = parseAniChartMedia(mediaObj, preferredLang, explicitAiringAt = airingAt, explicitNextEp = episode, explicitTimeUntil = timeUntil)
            if (media != null && (!hideAdult || !media.isAdult)) {
                val day = AniChartDay.fromEpochSeconds(airingAt)
                resultMap[day]?.add(media)
            }
        }

        resultMap
    }

    private fun parseAniChartMedia(
        obj: JsonObject,
        preferredLang: String,
        currentSeason: AniChartSeason? = null,
        currentYear: Int? = null,
        explicitAiringAt: Long? = null,
        explicitNextEp: Int? = null,
        explicitTimeUntil: Long? = null,
    ): AniChartMedia? {
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
        val nextEp = explicitNextEp ?: nextAirObj?.get("episode").asIntOrNull()
        val airingAt = explicitAiringAt ?: nextAirObj?.get("airingAt").asLongOrNull()
        val timeUntil = explicitTimeUntil ?: nextAirObj?.get("timeUntilAiring").asLongOrNull()

        val startObj = obj["startDate"].asJsonObjectOrNull()
        val sYear = startObj?.get("year").asIntOrNull()
        val sMonth = startObj?.get("month").asIntOrNull()
        val sDay = startObj?.get("day").asIntOrNull()
        val startDateStr = if (sYear != null && sMonth != null && sDay != null) {
            "${sYear.toString().padStart(4, '0')}-${sMonth.toString().padStart(2, '0')}-${sDay.toString().padStart(2, '0')}"
        } else null

        // Continuing anime = started before current season/year but still RELEASING
        val isContinuing = if (currentYear != null && sYear != null) {
            sYear < currentYear || (status == "RELEASING" && nextEp != null && nextEp > 13)
        } else false

        return AniChartMedia(
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
            isContinuing = isContinuing,
            isAdult = isAdult,
        )
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = if (this is JsonObject) this else null
    private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = if (this is JsonArray) this else null
    private fun JsonElement?.asStringOrNull(): String? = if (this is JsonPrimitive && this !is JsonNull) this.contentOrNull else null
    private fun JsonElement?.asIntOrNull(): Int? = if (this is JsonPrimitive) this.intOrNull else null
    private fun JsonElement?.asLongOrNull(): Long? = if (this is JsonPrimitive) this.longOrNull else null
    private fun JsonElement?.asDoubleOrNull(): Double? = if (this is JsonPrimitive) this.doubleOrNull else null
}
