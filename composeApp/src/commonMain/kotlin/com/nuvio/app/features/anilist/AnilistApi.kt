package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
import kotlin.math.roundToInt

object AnilistApi {
    private val log = Logger.withTag("AnilistApi")
    private const val GRAPHQL_ENDPOINT = "https://graphql.anilist.co"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    var lastDebugLog: String = "No requests made yet"
        private set

    suspend fun executeGraphQL(
        query: String,
        variables: JsonObject = buildJsonObject {},
        token: String? = null,
    ): JsonObject? {
        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }.toString()

        val headers = mutableMapOf(
            "User-Agent" to "Nuvio-Kai/1.0",
        )
        if (!token.isNullOrBlank()) {
            val sanitized = token.trim().removePrefix("Bearer ").trim()
            headers["Authorization"] = "Bearer $sanitized"
        }

        var responseText: String? = null
        try {
            responseText = httpPostJsonWithHeaders(
                url = GRAPHQL_ENDPOINT,
                body = payload,
                headers = headers,
            )
            lastDebugLog = "HTTP POST $GRAPHQL_ENDPOINT\nPayload: $payload\nResponse: ${responseText?.take(400)}"
        } catch (e: Exception) {
            lastDebugLog = "HTTP POST $GRAPHQL_ENDPOINT failed: ${e.message}\nPayload: $payload"
            log.w(e) { "executeGraphQL request failed: ${e.message}" }
            return null
        }

        if (responseText.isNullOrBlank()) {
            lastDebugLog = "GraphQL response empty for payload: $payload"
            return null
        }

        return try {
            val root = json.parseToJsonElement(responseText).jsonObject
            val errors = root["errors"]?.jsonArray
            if (errors != null && errors.isNotEmpty()) {
                val errMsg = errors.firstOrNull()?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                lastDebugLog = "GraphQL Error: $errMsg\nRaw: $responseText"
            }
            root
        } catch (e: Exception) {
            lastDebugLog = "Failed to parse GraphQL response: ${e.message}\nRaw: $responseText"
            log.w(e) { "Failed to parse GraphQL response JSON: $responseText" }
            null
        }
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? =
        if (this is JsonObject) this else null

    private fun JsonElement?.asJsonArrayOrNull(): JsonArray? =
        if (this is JsonArray) this else null

    private fun JsonElement?.asStringOrNull(): String? =
        if (this is JsonPrimitive && this !is JsonNull) this.contentOrNull else null

    private fun JsonElement?.asIntOrNull(): Int? =
        if (this is JsonPrimitive && this !is JsonNull) (this.intOrNull ?: this.contentOrNull?.toIntOrNull()) else null

    private fun JsonElement?.asDoubleOrNull(): Double? =
        if (this is JsonPrimitive && this !is JsonNull) (this.doubleOrNull ?: this.contentOrNull?.toDoubleOrNull()) else null

    private fun JsonElement?.asLongOrNull(): Long? =
        if (this is JsonPrimitive && this !is JsonNull) (this.longOrNull ?: this.contentOrNull?.toLongOrNull()) else null

    suspend fun fetchCurrentUser(token: String): AnilistUser? {
        val query = """
            query {
              Viewer {
                id
                name
                avatar {
                  large
                  medium
                }
              }
            }
        """.trimIndent()

        val root = executeGraphQL(query = query, token = token) ?: return null
        val viewer = root["data"].asJsonObjectOrNull()?.get("Viewer").asJsonObjectOrNull() ?: return null
        val id = viewer["id"].asIntOrNull() ?: return null
        val name = viewer["name"].asStringOrNull() ?: return null
        val avatar = viewer["avatar"].asJsonObjectOrNull()
        val avatarUrl = avatar?.get("large").asStringOrNull()
            ?: avatar?.get("medium").asStringOrNull()

        return AnilistUser(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
        )
    }

    suspend fun fetchUserAnimeList(
        token: String? = null,
        userName: String? = null,
        status: AnilistMediaListStatus? = null,
        page: Int = 1,
        perPage: Int = 25,
    ): List<AnilistMedia> {
        val query = """
            query FetchUserAnimeList(${'$'}userName: String, ${'$'}status: MediaListStatus, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  hasNextPage
                }
                mediaList(userName: ${'$'}userName, type: ANIME, status: ${'$'}status, sort: [UPDATED_TIME_DESC]) {
                  id
                  status
                  score
                  progress
                  repeat
                  updatedAt
                  media {
                    id
                    idMal
                    title {
                      romaji
                      english
                      native
                    }
                    format
                    status
                    episodes
                    duration
                    coverImage {
                      extraLarge
                      large
                      medium
                    }
                    bannerImage
                    genres
                    averageScore
                    description(asHtml: false)
                    startDate {
                      year
                    }
                    endDate {
                      year
                    }
                    relations {
                      edges {
                        relationType
                        node {
                          id
                          title {
                            english
                            romaji
                            native
                          }
                          format
                          episodes
                        }
                      }
                    }
                    nextAiringEpisode {
                      episode
                      airingAt
                      timeUntilAiring
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            if (!userName.isNullOrBlank()) put("userName", userName)
            if (status != null) put("status", status.name)
            put("page", page)
            put("perPage", perPage)
        }

        val root = executeGraphQL(query = query, variables = variables, token = token)
        val mediaListArray = root?.get("data")
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("mediaList")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaListArray.mapNotNull { itemElement ->
            val itemObj = itemElement.asJsonObjectOrNull() ?: return@mapNotNull null
            val mediaObj = itemObj["media"].asJsonObjectOrNull() ?: return@mapNotNull null
            val parsedMedia = parseMedia(mediaObj) ?: return@mapNotNull null
            val entry = parseMediaListEntry(itemObj, defaultMediaId = parsedMedia.id)
            parsedMedia.copy(mediaListEntry = entry)
        }
    }

    suspend fun fetchTrendingAnime(
        page: Int = 1,
        perPage: Int = 25,
    ): List<AnilistMedia> {
        val query = """
            query FetchTrendingAnime(${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  hasNextPage
                }
                media(type: ANIME, sort: [TRENDING_DESC, POPULARITY_DESC]) {
                  id
                  idMal
                  title {
                    romaji
                    english
                    native
                  }
                  format
                  status
                  episodes
                  duration
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  genres
                  averageScore
                  description(asHtml: false)
                  startDate {
                    year
                  }
                  endDate {
                    year
                  }
                  relations {
                    edges {
                      relationType
                      node {
                        id
                        title {
                          english
                          romaji
                          native
                        }
                        format
                        episodes
                      }
                    }
                  }
                  nextAiringEpisode {
                    episode
                    airingAt
                    timeUntilAiring
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("page", page)
            put("perPage", perPage)
        }

        val root = executeGraphQL(query = query, variables = variables)
        val mediaArray = root?.get("data")
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("media")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaArray.mapNotNull { it.asJsonObjectOrNull()?.let { obj -> parseMedia(obj) } }
    }

    suspend fun fetchPopularSeasonAnime(
        season: String? = null,
        seasonYear: Int? = null,
        page: Int = 1,
        perPage: Int = 25,
    ): List<AnilistMedia> {
        val query = """
            query FetchPopularSeasonAnime(${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  hasNextPage
                }
                media(type: ANIME, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: [POPULARITY_DESC]) {
                  id
                  idMal
                  title {
                    romaji
                    english
                    native
                  }
                  format
                  status
                  episodes
                  duration
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  genres
                  averageScore
                  description(asHtml: false)
                  startDate {
                    year
                  }
                  endDate {
                    year
                  }
                  relations {
                    edges {
                      relationType
                      node {
                        id
                        title {
                          english
                          romaji
                          native
                        }
                        format
                        episodes
                      }
                    }
                  }
                  nextAiringEpisode {
                    episode
                    airingAt
                    timeUntilAiring
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            if (!season.isNullOrBlank()) put("season", season.uppercase())
            if (seasonYear != null && seasonYear > 0) put("seasonYear", seasonYear)
            put("page", page)
            put("perPage", perPage)
        }

        val root = executeGraphQL(query = query, variables = variables)
        val mediaArray = root?.get("data")
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("media")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaArray.mapNotNull { it.asJsonObjectOrNull()?.let { obj -> parseMedia(obj) } }
    }

    suspend fun fetchTopRatedAnime(
        page: Int = 1,
        perPage: Int = 25,
    ): List<AnilistMedia> {
        val query = """
            query FetchTopRatedAnime(${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  hasNextPage
                }
                media(type: ANIME, sort: [SCORE_DESC]) {
                  id
                  idMal
                  title {
                    romaji
                    english
                    native
                  }
                  format
                  status
                  episodes
                  duration
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  genres
                  averageScore
                  description(asHtml: false)
                  startDate {
                    year
                  }
                  endDate {
                    year
                  }
                  relations {
                    edges {
                      relationType
                      node {
                        id
                        title {
                          english
                          romaji
                          native
                        }
                        format
                        episodes
                      }
                    }
                  }
                  nextAiringEpisode {
                    episode
                    airingAt
                    timeUntilAiring
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("page", page)
            put("perPage", perPage)
        }

        val root = executeGraphQL(query = query, variables = variables)
        val mediaArray = root?.get("data")
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("media")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaArray.mapNotNull { it.asJsonObjectOrNull()?.let { obj -> parseMedia(obj) } }
    }

    suspend fun fetchAiringAnime(
        page: Int = 1,
        perPage: Int = 25,
    ): List<AnilistMedia> {
        val query = """
            query FetchAiringAnime(${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  hasNextPage
                }
                media(type: ANIME, status: RELEASING, sort: [POPULARITY_DESC]) {
                  id
                  idMal
                  title {
                    romaji
                    english
                    native
                  }
                  format
                  status
                  episodes
                  duration
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  genres
                  averageScore
                  description(asHtml: false)
                  startDate {
                    year
                  }
                  endDate {
                    year
                  }
                  relations {
                    edges {
                      relationType
                      node {
                        id
                        title {
                          english
                          romaji
                          native
                        }
                        format
                        episodes
                      }
                    }
                  }
                  nextAiringEpisode {
                    episode
                    airingAt
                    timeUntilAiring
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("page", page)
            put("perPage", perPage)
        }

        val root = executeGraphQL(query = query, variables = variables)
        val mediaArray = root?.get("data")
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("media")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaArray.mapNotNull { it.asJsonObjectOrNull()?.let { obj -> parseMedia(obj) } }
    }

    suspend fun searchAnime(
        query: String,
        token: String? = null,
    ): List<AnilistMedia> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val graphQLQuery = """
            query SearchAnime(${'$'}search: String) {
              Page(page: 1, perPage: 25) {
                media(search: ${'$'}search, type: ANIME, sort: [SEARCH_MATCH, POPULARITY_DESC]) {
                  id
                  idMal
                  title {
                    romaji
                    english
                    native
                  }
                  format
                  status
                  episodes
                  duration
                  coverImage {
                    extraLarge
                    large
                    medium
                  }
                  bannerImage
                  genres
                  averageScore
                  description(asHtml: false)
                  startDate {
                    year
                  }
                  endDate {
                    year
                  }
                  relations {
                    edges {
                      relationType
                      node {
                        id
                        title {
                          english
                          romaji
                          native
                        }
                        format
                        episodes
                      }
                    }
                  }
                  nextAiringEpisode {
                    episode
                    airingAt
                    timeUntilAiring
                  }
                  mediaListEntry {
                    id
                    status
                    score
                    progress
                    repeat
                    updatedAt
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("search", cleanQuery)
        }

        val root = executeGraphQL(query = graphQLQuery, variables = variables, token = token)
        val mediaList = root?.get("data")
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("media")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaList.mapNotNull { element ->
            element.asJsonObjectOrNull()?.let { parseMedia(it) }
        }
    }

    suspend fun fetchStaffDetail(
        staffId: Int? = null,
        searchName: String? = null,
    ): com.nuvio.app.features.details.PersonDetail? {
        val cleanSearch = searchName?.trim()?.takeIf { it.isNotBlank() }
        if (staffId == null && cleanSearch == null) return null

        // 1. Try querying Staff first (by ID if available, or by search name)
        var staffResult = queryStaffEntity(staffId = staffId, searchName = cleanSearch)

        // If ID lookup returned null or search wasn't tried yet, try search by name
        if (staffResult == null && cleanSearch != null && staffId != null) {
            staffResult = queryStaffEntity(staffId = null, searchName = cleanSearch)
        }

        // 2. If Staff was not found, fallback to querying Character (by ID or search name)
        return staffResult ?: queryCharacterEntity(charId = staffId, searchName = cleanSearch)
    }

    private suspend fun queryStaffEntity(
        staffId: Int? = null,
        searchName: String? = null,
    ): com.nuvio.app.features.details.PersonDetail? {
        val query = if (staffId != null && staffId > 0) {
            """
            query (${'$'}id: Int) {
              Staff(id: ${'$'}id) {
                id
                name {
                  full
                  native
                }
                image {
                  large
                }
                description(asHtml: false)
                dateOfBirth {
                  year
                  month
                  day
                }
                homeTown
                characterMedia(page: 1, perPage: 35, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
            }
            """.trimIndent()
        } else if (!searchName.isNullOrBlank()) {
            """
            query (${'$'}search: String) {
              Staff(search: ${'$'}search) {
                id
                name {
                  full
                  native
                }
                image {
                  large
                }
                description(asHtml: false)
                dateOfBirth {
                  year
                  month
                  day
                }
                homeTown
                characterMedia(page: 1, perPage: 35, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
            }
            """.trimIndent()
        } else return null

        val variables = buildJsonObject {
            if (staffId != null && staffId > 0) put("id", staffId)
            else if (!searchName.isNullOrBlank()) put("search", searchName)
        }

        val root = executeGraphQL(query = query, variables = variables, token = null) ?: return null
        val staffObj = root.get("data").asJsonObjectOrNull()?.get("Staff").asJsonObjectOrNull() ?: return null

        val id = staffObj["id"].asIntOrNull() ?: staffId ?: 0
        val nameObj = staffObj["name"].asJsonObjectOrNull()
        val fullName = nameObj?.get("full").asStringOrNull() ?: searchName.orEmpty()
        val photo = staffObj["image"].asJsonObjectOrNull()?.get("large").asStringOrNull()
        val bio = com.nuvio.app.core.format.cleanHtmlDescription(staffObj["description"].asStringOrNull())

        val dobObj = staffObj["dateOfBirth"].asJsonObjectOrNull()
        val dobYear = dobObj?.get("year").asIntOrNull()
        val dobMonth = dobObj?.get("month").asIntOrNull()
        val dobDay = dobObj?.get("day").asIntOrNull()
        val birthday = if (dobYear != null) {
            val m = (dobMonth ?: 1).toString().padStart(2, '0')
            val d = (dobDay ?: 1).toString().padStart(2, '0')
            "$dobYear-$m-$d"
        } else null

        val hometown = staffObj["homeTown"].asStringOrNull() ?: "Japan"

        val rawNodes = staffObj["characterMedia"].asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull() ?: JsonArray(emptyList())

        val seen = mutableSetOf<Int>()
        val total = rawNodes.size
        val mediaList = rawNodes.mapIndexedNotNull { idx, element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val mId = obj["id"].asIntOrNull() ?: return@mapIndexedNotNull null
            if (!seen.add(mId)) return@mapIndexedNotNull null

            val titleObj = obj["title"].asJsonObjectOrNull()
            val engTitle = titleObj?.get("english").asStringOrNull()
            val romTitle = titleObj?.get("romaji").asStringOrNull()
            val displayTitle = engTitle ?: romTitle ?: return@mapIndexedNotNull null

            val coverObj = obj["coverImage"].asJsonObjectOrNull()
            val poster = coverObj?.get("extraLarge").asStringOrNull() ?: coverObj?.get("large").asStringOrNull() ?: return@mapIndexedNotNull null
            val banner = obj["bannerImage"].asStringOrNull()
            val format = obj["format"].asStringOrNull()
            val episodes = obj["episodes"].asIntOrNull()
            val year = obj["startDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()

            val isMovie = format.equals("MOVIE", ignoreCase = true) || episodes == 1
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$mId",
                type = if (isMovie) "movie" else "series",
                name = displayTitle,
                poster = poster,
                banner = banner,
                logo = null,
                description = null,
                releaseInfo = if (episodes != null) "$episodes Ep" else year?.toString(),
                rawReleaseDate = if (year != null) "$year-01-01" else null,
                popularity = (total - idx).toDouble(),
            )
        }

        val movieCredits = mediaList.filter { it.type == "movie" }
        val tvCredits = mediaList.filter { it.type == "series" }

        return com.nuvio.app.features.details.PersonDetail(
            tmdbId = id,
            name = fullName,
            biography = bio,
            birthday = birthday,
            deathday = null,
            placeOfBirth = hometown,
            profilePhoto = photo,
            knownFor = "Voice Acting",
            movieCredits = movieCredits,
            tvCredits = tvCredits,
        )
    }

    private suspend fun queryCharacterEntity(
        charId: Int? = null,
        searchName: String? = null,
    ): com.nuvio.app.features.details.PersonDetail? {
        val query = if (charId != null && charId > 0) {
            """
            query (${'$'}id: Int) {
              Character(id: ${'$'}id) {
                id
                name {
                  full
                  native
                }
                image {
                  large
                }
                description(asHtml: false)
                dateOfBirth {
                  year
                  month
                  day
                }
                media(page: 1, perPage: 35, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
            }
            """.trimIndent()
        } else if (!searchName.isNullOrBlank()) {
            """
            query (${'$'}search: String) {
              Character(search: ${'$'}search) {
                id
                name {
                  full
                  native
                }
                image {
                  large
                }
                description(asHtml: false)
                dateOfBirth {
                  year
                  month
                  day
                }
                media(page: 1, perPage: 35, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
            }
            """.trimIndent()
        } else return null

        val variables = buildJsonObject {
            if (charId != null && charId > 0) put("id", charId)
            else if (!searchName.isNullOrBlank()) put("search", searchName)
        }

        val root = executeGraphQL(query = query, variables = variables, token = null) ?: return null
        val charObj = root.get("data").asJsonObjectOrNull()?.get("Character").asJsonObjectOrNull() ?: return null

        val id = charObj["id"].asIntOrNull() ?: charId ?: 0
        val nameObj = charObj["name"].asJsonObjectOrNull()
        val fullName = nameObj?.get("full").asStringOrNull() ?: searchName.orEmpty()
        val photo = charObj["image"].asJsonObjectOrNull()?.get("large").asStringOrNull()
        val bio = com.nuvio.app.core.format.cleanHtmlDescription(charObj["description"].asStringOrNull())

        val dobObj = charObj["dateOfBirth"].asJsonObjectOrNull()
        val dobYear = dobObj?.get("year").asIntOrNull()
        val dobMonth = dobObj?.get("month").asIntOrNull()
        val dobDay = dobObj?.get("day").asIntOrNull()
        val birthday = if (dobYear != null) {
            val m = (dobMonth ?: 1).toString().padStart(2, '0')
            val d = (dobDay ?: 1).toString().padStart(2, '0')
            "$dobYear-$m-$d"
        } else null

        val rawNodes = charObj["media"].asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull() ?: JsonArray(emptyList())

        val seen = mutableSetOf<Int>()
        val total = rawNodes.size
        val mediaList = rawNodes.mapIndexedNotNull { idx, element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val mId = obj["id"].asIntOrNull() ?: return@mapIndexedNotNull null
            if (!seen.add(mId)) return@mapIndexedNotNull null

            val titleObj = obj["title"].asJsonObjectOrNull()
            val engTitle = titleObj?.get("english").asStringOrNull()
            val romTitle = titleObj?.get("romaji").asStringOrNull()
            val displayTitle = engTitle ?: romTitle ?: return@mapIndexedNotNull null

            val coverObj = obj["coverImage"].asJsonObjectOrNull()
            val poster = coverObj?.get("extraLarge").asStringOrNull() ?: coverObj?.get("large").asStringOrNull() ?: return@mapIndexedNotNull null
            val banner = obj["bannerImage"].asStringOrNull()
            val format = obj["format"].asStringOrNull()
            val episodes = obj["episodes"].asIntOrNull()
            val year = obj["startDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()

            val isMovie = format.equals("MOVIE", ignoreCase = true) || episodes == 1
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$mId",
                type = if (isMovie) "movie" else "series",
                name = displayTitle,
                poster = poster,
                banner = banner,
                logo = null,
                description = null,
                releaseInfo = if (episodes != null) "$episodes Ep" else year?.toString(),
                rawReleaseDate = if (year != null) "$year-01-01" else null,
                popularity = (total - idx).toDouble(),
            )
        }

        val movieCredits = mediaList.filter { it.type == "movie" }
        val tvCredits = mediaList.filter { it.type == "series" }

        return com.nuvio.app.features.details.PersonDetail(
            tmdbId = id,
            name = fullName,
            biography = bio,
            birthday = birthday,
            deathday = null,
            placeOfBirth = "Japan",
            profilePhoto = photo,
            knownFor = "Character",
            movieCredits = movieCredits,
            tvCredits = tvCredits,
        )
    }

    suspend fun fetchStaffMedia(search: String): List<com.nuvio.app.features.home.MetaPreview> {
        val cleanSearch = search.trim()
        if (cleanSearch.isBlank()) return emptyList()

        val graphQLQuery = """
            query (${'$'}search: String) {
              Staff(search: ${'$'}search) {
                id
                name {
                  full
                  native
                }
                characterMedia(page: 1, perPage: 35, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
              Character(search: ${'$'}search) {
                id
                name {
                  full
                  native
                }
                media(page: 1, perPage: 35, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("search", cleanSearch)
        }

        val root = executeGraphQL(query = graphQLQuery, variables = variables, token = null) ?: return emptyList()
        val dataObj = root.get("data").asJsonObjectOrNull()
        val staffNodes = dataObj?.get("Staff")
            .asJsonObjectOrNull()?.get("characterMedia")
            .asJsonObjectOrNull()?.get("nodes")
            .asJsonArrayOrNull()

        val charNodes = dataObj?.get("Character")
            .asJsonObjectOrNull()?.get("media")
            .asJsonObjectOrNull()?.get("nodes")
            .asJsonArrayOrNull()

        val rawNodes = (staffNodes?.takeIf { it.isNotEmpty() } ?: charNodes) ?: return emptyList()

        val seen = mutableSetOf<Int>()
        val total = rawNodes.size
        return rawNodes.mapIndexedNotNull { idx, element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val id = obj["id"].asIntOrNull() ?: return@mapIndexedNotNull null
            if (!seen.add(id)) return@mapIndexedNotNull null

            val titleObj = obj["title"].asJsonObjectOrNull()
            val engTitle = titleObj?.get("english").asStringOrNull()
            val romTitle = titleObj?.get("romaji").asStringOrNull()
            val displayTitle = engTitle ?: romTitle ?: return@mapIndexedNotNull null

            val coverObj = obj["coverImage"].asJsonObjectOrNull()
            val poster = coverObj?.get("extraLarge").asStringOrNull() ?: coverObj?.get("large").asStringOrNull() ?: return@mapIndexedNotNull null
            val banner = obj["bannerImage"].asStringOrNull()
            val format = obj["format"].asStringOrNull()
            val episodes = obj["episodes"].asIntOrNull()
            val year = obj["startDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()

            val isMovie = format.equals("MOVIE", ignoreCase = true) || episodes == 1
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$id",
                type = if (isMovie) "movie" else "series",
                name = displayTitle,
                poster = poster,
                banner = banner,
                logo = null,
                description = null,
                releaseInfo = if (episodes != null) "$episodes Ep" else year?.toString(),
                rawReleaseDate = if (year != null) "$year-01-01" else null,
                popularity = (total - idx).toDouble(),
            )
        }
    }

    suspend fun fetchStudioMedia(search: String): List<com.nuvio.app.features.home.MetaPreview> {
        val cleanSearch = search.trim()
        if (cleanSearch.isBlank()) return emptyList()

        val graphQLQuery = """
            query (${'$'}search: String) {
              Studio(search: ${'$'}search) {
                id
                name
                media(page: 1, perPage: 50, sort: POPULARITY_DESC) {
                  nodes {
                    id
                    title {
                      english
                      romaji
                    }
                    coverImage {
                      extraLarge
                      large
                    }
                    bannerImage
                    episodes
                    format
                    startDate {
                      year
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("search", cleanSearch)
        }

        val root = executeGraphQL(query = graphQLQuery, variables = variables, token = null) ?: return emptyList()
        val nodes = root.get("data")
            .asJsonObjectOrNull()?.get("Studio")
            .asJsonObjectOrNull()?.get("media")
            .asJsonObjectOrNull()?.get("nodes")
            .asJsonArrayOrNull() ?: return emptyList()

        val seen = mutableSetOf<Int>()
        val total = nodes.size
        return nodes.mapIndexedNotNull { idx, element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            val id = obj["id"].asIntOrNull() ?: return@mapIndexedNotNull null
            if (!seen.add(id)) return@mapIndexedNotNull null

            val titleObj = obj["title"].asJsonObjectOrNull()
            val engTitle = titleObj?.get("english").asStringOrNull()
            val romTitle = titleObj?.get("romaji").asStringOrNull()
            val displayTitle = engTitle ?: romTitle ?: return@mapIndexedNotNull null

            val coverObj = obj["coverImage"].asJsonObjectOrNull()
            val poster = coverObj?.get("extraLarge").asStringOrNull() ?: coverObj?.get("large").asStringOrNull() ?: return@mapIndexedNotNull null
            val banner = obj["bannerImage"].asStringOrNull()
            val format = obj["format"].asStringOrNull()
            val episodes = obj["episodes"].asIntOrNull()
            val year = obj["startDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()

            val isMovie = format.equals("MOVIE", ignoreCase = true) || episodes == 1
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$id",
                type = if (isMovie) "movie" else "series",
                name = displayTitle,
                poster = poster,
                banner = banner,
                logo = null,
                description = null,
                releaseInfo = if (episodes != null) "$episodes Ep" else year?.toString(),
                rawReleaseDate = if (year != null) "$year-01-01" else null,
                popularity = (total - idx).toDouble(),
            )
        }
    }

    suspend fun resolveArmAnilistId(source: String, id: String): Int? {
        val url = "https://arm.haglund.dev/api/v2/ids?source=$source&id=$id&include=anilist"
        return runCatching {
            val responseText = com.nuvio.app.features.addons.httpGetText(url)
            val jsonElement = json.parseToJsonElement(responseText)
            jsonElement.asJsonObjectOrNull()?.get("anilist").asIntOrNull()
        }.getOrNull()
    }

    suspend fun searchViaKitsu(query: String): Int? {
        val safeQuery = query.trim().replace(" ", "%20")
        val url = "https://kitsu.io/api/edge/anime?filter%5Btext%5D=$safeQuery&page%5Blimit%5D=1"
        return runCatching {
            val text = com.nuvio.app.features.addons.httpGetText(url)
            val jsonElement = json.parseToJsonElement(text)
            val dataArray = jsonElement.asJsonObjectOrNull()?.get("data").asJsonArrayOrNull()
            val firstItem = dataArray?.firstOrNull().asJsonObjectOrNull()
            val kitsuId = firstItem?.get("id").asStringOrNull()
            if (!kitsuId.isNullOrBlank()) {
                resolveArmAnilistId(source = "kitsu", id = kitsuId)
            } else null
        }.getOrNull()
    }

    suspend fun fetchMediaByMalId(
        malId: Int,
        token: String? = null,
    ): AnilistMedia? {
        val query = """
            query FetchMediaByMalId(${'$'}idMal: Int) {
              Media(idMal: ${'$'}idMal, type: ANIME) {
                id
                idMal
                title {
                  romaji
                  english
                  native
                }
                format
                status
                episodes
                duration
                coverImage {
                  extraLarge
                  large
                  medium
                }
                bannerImage
                genres
                averageScore
                description(asHtml: false)
                startDate {
                  year
                }
                endDate {
                  year
                }
                relations {
                  edges {
                    relationType
                    node {
                      id
                      title {
                        english
                        romaji
                        native
                      }
                      format
                      episodes
                    }
                  }
                }
                nextAiringEpisode {
                  episode
                  airingAt
                  timeUntilAiring
                }
                mediaListEntry {
                  id
                  status
                  score
                  progress
                  repeat
                  updatedAt
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject { put("idMal", malId) }
        val root = executeGraphQL(query = query, variables = variables, token = token)
        val mediaObj = root?.get("data").asJsonObjectOrNull()?.get("Media").asJsonObjectOrNull() ?: return null
        return parseMedia(mediaObj)
    }

    data class MalMetadata(
        val score: Double?,
        val ageRating: String?,
    )

    private val malMetadataCache = mutableMapOf<Int, MalMetadata>()
    private val malScoreRegex = Regex("itemprop=[\"']ratingValue[\"'][^>]*>([0-9.]+)|class=[\"']score-label[^\"']*[\"']>([0-9.]+)")
    private val malAgeRatingRegex = Regex("Rating:</span>\\s*([^<\\n\\r]+)")

    private fun normalizeAnimeAgeRating(raw: String): String {
        val clean = raw.trim()
        return when {
            clean.startsWith("PG-13", ignoreCase = true) -> "TV-14"
            clean.startsWith("R - 17+", ignoreCase = true) || clean.startsWith("R-17+", ignoreCase = true) -> "TV-MA"
            clean.startsWith("R+", ignoreCase = true) -> "TV-MA"
            clean.startsWith("Rx", ignoreCase = true) || clean.startsWith("18+", ignoreCase = true) -> "18+"
            clean.startsWith("PG", ignoreCase = true) -> "PG"
            clean.startsWith("G", ignoreCase = true) -> "G"
            else -> clean.substringBefore("-").trim().ifEmpty { clean }
        }
    }

    suspend fun fetchMalMetadata(idMal: Int): MalMetadata? = withContext(Dispatchers.Default) {
        if (idMal <= 0) return@withContext null
        malMetadataCache[idMal]?.let { return@withContext it }

        val directMeta = runCatching {
            val url = "https://myanimelist.net/anime/$idMal"
            val text = com.nuvio.app.features.addons.httpGetText(url) ?: return@runCatching null
            val scoreMatch = malScoreRegex.find(text)
            val scoreStr = scoreMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: scoreMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }
            val score = scoreStr?.toDoubleOrNull()

            val ratingMatch = malAgeRatingRegex.find(text)
            val rawRating = ratingMatch?.groupValues?.getOrNull(1)?.trim()
            val ageRating = rawRating?.let { normalizeAnimeAgeRating(it) }

            MalMetadata(score = score, ageRating = ageRating)
        }.getOrNull()

        val finalMeta = if (directMeta != null && directMeta.score != null) {
            directMeta
        } else {
            runCatching {
                val url = "https://api.jikan.moe/v4/anime/$idMal"
                val text = com.nuvio.app.features.addons.httpGetText(url) ?: return@runCatching null
                val root = json.parseToJsonElement(text).asJsonObjectOrNull() ?: return@runCatching null
                val dataObj = root["data"]?.asJsonObjectOrNull()
                val score = dataObj?.get("score")?.asDoubleOrNull() ?: directMeta?.score
                val rawRating = dataObj?.get("rating")?.asStringOrNull()
                val ageRating = rawRating?.let { normalizeAnimeAgeRating(it) } ?: directMeta?.ageRating
                MalMetadata(score = score, ageRating = ageRating)
            }.getOrNull() ?: directMeta
        }

        if (finalMeta != null) {
            malMetadataCache[idMal] = finalMeta
        }
        finalMeta
    }

    suspend fun fetchMalScore(idMal: Int): Double? = fetchMalMetadata(idMal)?.score

    private val mediaCache = mutableMapOf<Int, AnilistMedia>()

    fun getCachedMedia(mediaId: Int): AnilistMedia? = mediaCache[mediaId]

    suspend fun fetchMediaById(
        mediaId: Int,
        token: String? = null,
    ): AnilistMedia? {
        val cached = mediaCache[mediaId]
        if (cached != null && cached.isFullDetails && (!token.isNullOrBlank() == (cached.mediaListEntry != null) || token.isNullOrBlank())) {
            return cached
        }
        val hasAuth = !token.isNullOrBlank()
        val mediaListEntryBlock = if (hasAuth) {
            """
            mediaListEntry {
              id
              status
              score
              progress
              repeat
              updatedAt
            }
            """.trimIndent()
        } else ""

        val query = """
            query FetchMediaById(${'$'}id: Int) {
              Media(id: ${'$'}id) {
                id
                idMal
                title {
                  romaji
                  english
                  native
                }
                format
                status
                episodes
                duration
                coverImage {
                  extraLarge
                  large
                  medium
                }
                bannerImage
                genres
                averageScore
                description(asHtml: false)
                nextAiringEpisode {
                  episode
                  airingAt
                  timeUntilAiring
                }
                streamingEpisodes {
                  title
                  thumbnail
                  url
                  site
                }
                characters(sort: RELEVANCE, perPage: 35) {
                  edges {
                    role
                    node {
                      id
                      name {
                        full
                      }
                      image {
                        large
                      }
                    }
                    voiceActors(sort: [RELEVANCE, ID]) {
                      id
                      name {
                        full
                      }
                      image {
                        large
                      }
                      languageV2
                    }
                  }
                }
                studios {
                  nodes {
                    id
                    name
                    isAnimationStudio
                  }
                }
                recommendations(sort: RATING_DESC, perPage: 12) {
                  nodes {
                    mediaRecommendation {
                      id
                      title {
                        english
                        romaji
                      }
                      format
                      episodes
                      coverImage {
                        extraLarge
                        large
                        medium
                      }
                      bannerImage
                      averageScore
                    }
                  }
                }
                trailer {
                  id
                  site
                }
                startDate {
                  year
                  month
                  day
                }
                endDate {
                  year
                }
                airingSchedule(perPage: 50) {
                  nodes {
                    episode
                    airingAt
                  }
                }
                staff(perPage: 25) {
                  edges {
                    role
                    node {
                      id
                      name {
                        full
                      }
                      image {
                        large
                      }
                    }
                  }
                }
                relations {
                  edges {
                    relationType
                    node {
                      id
                      title {
                        english
                        romaji
                        native
                      }
                      format
                      episodes
                      status
                      coverImage {
                        extraLarge
                        large
                        medium
                      }
                      bannerImage
                      averageScore
                      relations {
                        edges {
                          relationType
                          node {
                            id
                            title {
                              english
                              romaji
                              native
                            }
                            format
                            episodes
                            status
                            bannerImage
                            averageScore
                          }
                        }
                      }
                    }
                  }
                }
                $mediaListEntryBlock
              }
            }
        """.trimIndent()

        val variables = buildJsonObject { put("id", mediaId) }
        val root = executeGraphQL(query = query, variables = variables, token = token)
        val mediaObj = root?.get("data").asJsonObjectOrNull()?.get("Media").asJsonObjectOrNull() ?: return null
        return parseMedia(mediaObj, isFullDetails = true)
    }

    suspend fun saveMediaListEntry(
        mediaId: Int,
        status: AnilistMediaListStatus? = null,
        progress: Int? = null,
        score: Double? = null,
        token: String,
    ): AnilistMediaListEntry? {
        val mutation = """
            mutation SaveMediaListEntry(${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int, ${'$'}score: Float) {
              SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress, score: ${'$'}score) {
                id
                mediaId
                status
                score
                progress
                repeat
                updatedAt
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            if (status != null) {
                put("status", status.name)
            }
            if (progress != null) {
                put("progress", progress)
            }
            if (score != null) {
                put("score", score)
            }
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        val entryObj = root?.get("data").asJsonObjectOrNull()?.get("SaveMediaListEntry").asJsonObjectOrNull() ?: return null
        return parseMediaListEntry(entryObj, defaultMediaId = mediaId)
    }

    suspend fun deleteMediaListEntry(
        entryId: Int,
        token: String,
    ): Boolean {
        val mutation = """
            mutation DeleteMediaListEntry(${'$'}id: Int) {
              DeleteMediaListEntry(id: ${'$'}id) {
                deleted
              }
            }
        """.trimIndent()

        val variables = buildJsonObject { put("id", entryId) }
        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        return root?.get("data").asJsonObjectOrNull()?.get("DeleteMediaListEntry").asJsonObjectOrNull()?.get("deleted").asStringOrNull() == "true"
    }

    private fun parseMedia(obj: JsonObject, isFullDetails: Boolean = false): AnilistMedia? = runCatching {
        val id = obj["id"].asIntOrNull() ?: return null
        val idMal = obj["idMal"].asIntOrNull()
        val format = obj["format"].asStringOrNull()
        val status = obj["status"].asStringOrNull()
        val episodes = obj["episodes"].asIntOrNull()
        val duration = obj["duration"].asIntOrNull()
        val bannerImage = obj["bannerImage"].asStringOrNull()
        val averageScore = obj["averageScore"].asIntOrNull()
        val description = com.nuvio.app.core.format.cleanHtmlDescription(obj["description"].asStringOrNull())

        val titleObj = obj["title"].asJsonObjectOrNull()
        val title = titleObj?.let {
            AnilistTitle(
                romaji = it["romaji"].asStringOrNull(),
                english = it["english"].asStringOrNull(),
                native = it["native"].asStringOrNull(),
            )
        }

        val coverObj = obj["coverImage"].asJsonObjectOrNull()
        val coverImage = coverObj?.let {
            AnilistCoverImage(
                extraLarge = it["extraLarge"].asStringOrNull(),
                large = it["large"].asStringOrNull(),
                medium = it["medium"].asStringOrNull(),
            )
        }

        val genres = obj["genres"].asJsonArrayOrNull()?.mapNotNull { it.asStringOrNull() }.orEmpty()

        val nextObj = obj["nextAiringEpisode"].asJsonObjectOrNull()
        val nextAiringEpisode = nextObj?.let {
            val ep = it["episode"].asIntOrNull()
            val airAt = it["airingAt"].asLongOrNull()
            val timeUntil = it["timeUntilAiring"].asLongOrNull()
            if (ep != null && airAt != null && timeUntil != null) {
                AnilistNextAiringEpisode(ep, airAt, timeUntil)
            } else null
        }

        val streamingArray = obj["streamingEpisodes"].asJsonArrayOrNull()
        val streamingEpisodes = streamingArray?.mapNotNull { item ->
            val sObj = item.asJsonObjectOrNull() ?: return@mapNotNull null
            AnilistStreamingEpisode(
                title = sObj["title"].asStringOrNull(),
                thumbnail = sObj["thumbnail"].asStringOrNull(),
                url = sObj["url"].asStringOrNull(),
                site = sObj["site"].asStringOrNull(),
            )
        }.orEmpty()

        val characters = obj["characters"].asJsonObjectOrNull()
            ?.get("edges").asJsonArrayOrNull()
            ?.mapNotNull { edgeElem ->
                val edge = edgeElem.asJsonObjectOrNull() ?: return@mapNotNull null
                val role = edge["role"].asStringOrNull()
                val node = edge["node"].asJsonObjectOrNull()
                val charId = node?.get("id").asIntOrNull()
                val charName = node?.get("name").asJsonObjectOrNull()?.get("full").asStringOrNull()
                val charImg = node?.get("image").asJsonObjectOrNull()?.get("large").asStringOrNull()

                val vaList = edge["voiceActors"].asJsonArrayOrNull()?.mapNotNull { vaElem ->
                    val vaObj = vaElem.asJsonObjectOrNull() ?: return@mapNotNull null
                    val vId = vaObj["id"].asIntOrNull()
                    val vName = vaObj["name"].asJsonObjectOrNull()?.get("full").asStringOrNull() ?: return@mapNotNull null
                    val vImg = vaObj["image"].asJsonObjectOrNull()?.get("large").asStringOrNull()
                    val vLang = vaObj["languageV2"].asStringOrNull()
                    AnilistCharacterVoiceActor(id = vId, name = vName, image = vImg, language = vLang)
                }.orEmpty()

                val primaryVa = vaList.firstOrNull { it.language.equals("Japanese", ignoreCase = true) } ?: vaList.firstOrNull()
                if (charName != null || vaList.isNotEmpty()) {
                    AnilistCharacter(
                        id = charId,
                        name = charName,
                        role = role,
                        image = charImg,
                        voiceActor = primaryVa,
                        voiceActors = vaList,
                    )
                } else null
            }.orEmpty()

        val studios = obj["studios"].asJsonObjectOrNull()
            ?.get("nodes").asJsonArrayOrNull()
            ?.mapNotNull { studioElem ->
                val sObj = studioElem.asJsonObjectOrNull() ?: return@mapNotNull null
                val sName = sObj["name"].asStringOrNull() ?: return@mapNotNull null
                val sId = sObj["id"].asIntOrNull()
                val isAnim = sObj["isAnimationStudio"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                AnilistStudio(id = sId, name = sName, isAnimationStudio = isAnim)
            }.orEmpty()

        val recommendations = obj["recommendations"].asJsonObjectOrNull()
            ?.get("nodes").asJsonArrayOrNull()
            ?.mapNotNull { recElem ->
                val recMedia = recElem.asJsonObjectOrNull()?.get("mediaRecommendation").asJsonObjectOrNull() ?: return@mapNotNull null
                val recId = recMedia["id"].asIntOrNull() ?: return@mapNotNull null
                val recTitleObj = recMedia["title"].asJsonObjectOrNull()
                val recTitle = recTitleObj?.let {
                    AnilistTitle(
                        romaji = it["romaji"].asStringOrNull(),
                        english = it["english"].asStringOrNull(),
                    )
                }
                val recCoverObj = recMedia["coverImage"].asJsonObjectOrNull()
                val recCover = recCoverObj?.let {
                    AnilistCoverImage(
                        extraLarge = it["extraLarge"].asStringOrNull(),
                        large = it["large"].asStringOrNull(),
                        medium = it["medium"].asStringOrNull(),
                    )
                }
                AnilistRecommendation(
                    id = recId,
                    title = recTitle,
                    format = recMedia["format"].asStringOrNull(),
                    episodes = recMedia["episodes"].asIntOrNull(),
                    coverImage = recCover,
                    bannerImage = recMedia["bannerImage"].asStringOrNull(),
                    averageScore = recMedia["averageScore"].asIntOrNull(),
                )
            }.orEmpty()

        val trailerObj = obj["trailer"].asJsonObjectOrNull()
        val trailer = trailerObj?.let {
            val tId = it["id"].asStringOrNull()
            val tSite = it["site"].asStringOrNull()
            if (tId != null) AnilistTrailerInfo(id = tId, site = tSite) else null
        }

        val staff = obj["staff"].asJsonObjectOrNull()
            ?.get("edges").asJsonArrayOrNull()
            ?.mapNotNull { staffElem ->
                val sObj = staffElem.asJsonObjectOrNull() ?: return@mapNotNull null
                val sRole = sObj["role"].asStringOrNull()
                val sNode = sObj["node"].asJsonObjectOrNull()
                val sId = sNode?.get("id").asIntOrNull()
                val sName = sNode?.get("name").asJsonObjectOrNull()?.get("full").asStringOrNull()
                val sImg = sNode?.get("image").asJsonObjectOrNull()?.get("large").asStringOrNull()
                if (sName != null) AnilistStaff(id = sId, name = sName, role = sRole, image = sImg) else null
            }.orEmpty()

        val startDateObj = obj["startDate"].asJsonObjectOrNull()
        val startYear = startDateObj?.get("year").asIntOrNull()
        val startMonth = startDateObj?.get("month").asIntOrNull()
        val startDay = startDateObj?.get("day").asIntOrNull()
        val endYear = obj["endDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()

        val scheduleNodes = obj["airingSchedule"].asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull()
        val airingScheduleMap = scheduleNodes?.mapNotNull { item ->
            val node = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val ep = node["episode"].asIntOrNull() ?: return@mapNotNull null
            val airingAt = node["airingAt"].asLongOrNull() ?: return@mapNotNull null
            ep to airingAt
        }?.toMap().orEmpty()

        val relations = parseRelations(obj)

        val entryObj = obj["mediaListEntry"].asJsonObjectOrNull()
        val mediaListEntry = entryObj?.let { parseMediaListEntry(it, defaultMediaId = id) }

        val media = AnilistMedia(
            id = id,
            idMal = idMal,
            title = title,
            format = format,
            status = status,
            episodes = episodes,
            duration = duration,
            coverImage = coverImage,
            bannerImage = bannerImage,
            genres = genres,
            averageScore = averageScore,
            description = description,
            nextAiringEpisode = nextAiringEpisode,
            streamingEpisodes = streamingEpisodes,
            characters = characters,
            studios = studios,
            recommendations = recommendations,
            trailer = trailer,
            staff = staff,
            startDateYear = startYear,
            startDateMonth = startMonth,
            startDateDay = startDay,
            endDateYear = endYear,
            airingSchedule = airingScheduleMap,
            mediaListEntry = mediaListEntry,
            relations = relations,
            isFullDetails = isFullDetails,
        )
        val existing = mediaCache[id]
        if (isFullDetails || existing == null || !existing.isFullDetails) {
            mediaCache[id] = media
        }
        media
    }.getOrNull()

    private fun parseMediaListEntry(obj: JsonObject, defaultMediaId: Int): AnilistMediaListEntry = runCatching {
        val id = obj["id"].asIntOrNull() ?: 0
        val mediaId = obj["mediaId"].asIntOrNull() ?: defaultMediaId
        val statusStr = obj["status"].asStringOrNull()
        val score = obj["score"].asDoubleOrNull() ?: 0.0
        val progress = obj["progress"].asIntOrNull() ?: 0
        val repeat = obj["repeat"].asIntOrNull() ?: 0
        val updatedAt = obj["updatedAt"].asLongOrNull() ?: 0L

        AnilistMediaListEntry(
            id = id,
            mediaId = mediaId,
            status = AnilistMediaListStatus.fromString(statusStr),
            score = score,
            progress = progress,
            repeat = repeat,
            updatedAt = updatedAt,
        )
    }.getOrElse {
        AnilistMediaListEntry(
            id = 0,
            mediaId = defaultMediaId,
            status = null,
            score = 0.0,
            progress = 0,
            repeat = 0,
            updatedAt = 0L,
        )
    }

    private fun parseRelations(obj: JsonObject): List<AnilistRelation> {
        val relationsObj = obj["relations"].asJsonObjectOrNull() ?: return emptyList()
        val edges = relationsObj["edges"].asJsonArrayOrNull() ?: return emptyList()
        return edges.mapNotNull { edgeElem ->
            val edge = edgeElem.asJsonObjectOrNull() ?: return@mapNotNull null
            val relType = edge["relationType"].asStringOrNull()
            val node = edge["node"].asJsonObjectOrNull() ?: return@mapNotNull null
            val nId = node["id"].asIntOrNull() ?: return@mapNotNull null
            val nTitleObj = node["title"].asJsonObjectOrNull()
            val nTitle = AnilistTitle(
                romaji = nTitleObj?.get("romaji").asStringOrNull(),
                english = nTitleObj?.get("english").asStringOrNull(),
                native = nTitleObj?.get("native").asStringOrNull(),
            )
            val covObj = node["coverImage"].asJsonObjectOrNull()
            val cov = covObj?.let {
                AnilistCoverImage(
                    extraLarge = it["extraLarge"].asStringOrNull(),
                    large = it["large"].asStringOrNull(),
                    medium = it["medium"].asStringOrNull(),
                )
            }
            val banner = node["bannerImage"].asStringOrNull()
            val status = node["status"].asStringOrNull()
            val avgScore = node["averageScore"].asIntOrNull()
            val nestedRelations = parseRelations(node)
            AnilistRelation(
                relationType = relType,
                id = nId,
                title = nTitle,
                format = node["format"].asStringOrNull(),
                episodes = node["episodes"].asIntOrNull(),
                status = status,
                coverImage = cov,
                bannerImage = banner,
                averageScore = avgScore,
                relations = nestedRelations,
            )
        }
    }

    suspend fun fetchMediaListCollection(userId: Int, token: String): List<AnilistLibraryItem> {
        val query = """
            query (${'$'}userId: Int) {
                MediaListCollection(userId: ${'$'}userId, type: ANIME) {
                    lists {
                        status
                        entries {
                            id
                            status
                            progress
                            score(format: POINT_100)
                            updatedAt
                            media {
                                id
                                idMal
                                episodes
                                status
                                format
                                title {
                                    english
                                    romaji
                                    native
                                    userPreferred
                                }
                                coverImage {
                                    extraLarge
                                    large
                                    medium
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("userId", userId)
        }

        return try {
            val root = executeGraphQL(query = query, variables = variables, token = token) ?: return emptyList()
            val collectionObj = root["data"].asJsonObjectOrNull()?.get("MediaListCollection").asJsonObjectOrNull() ?: return emptyList()
            val lists = collectionObj["lists"].asJsonArrayOrNull() ?: return emptyList()

            lists.flatMap { listGroupElem ->
                val listGroup = listGroupElem.asJsonObjectOrNull() ?: return@flatMap emptyList()
                val groupStatus = listGroup["status"].asStringOrNull().orEmpty()
                val entries = listGroup["entries"].asJsonArrayOrNull() ?: return@flatMap emptyList()

                entries.mapNotNull { entryElem ->
                    val entryObj = entryElem.asJsonObjectOrNull() ?: return@mapNotNull null
                    val mediaObj = entryObj["media"].asJsonObjectOrNull() ?: return@mapNotNull null
                    val mediaId = mediaObj["id"].asIntOrNull() ?: return@mapNotNull null
                    val titleObj = mediaObj["title"].asJsonObjectOrNull()
                    val titleModel = AnilistTitle(
                        romaji = titleObj?.get("romaji").asStringOrNull(),
                        english = titleObj?.get("english").asStringOrNull(),
                        native = titleObj?.get("native").asStringOrNull(),
                    )
                    val displayTitle = titleModel.getDisplayTitle()
                    val coverObj = mediaObj["coverImage"].asJsonObjectOrNull()
                    val posterUrl = coverObj?.get("extraLarge").asStringOrNull()
                        ?: coverObj?.get("large").asStringOrNull()
                        ?: coverObj?.get("medium").asStringOrNull()

                    val entryId = entryObj["id"].asIntOrNull() ?: 0
                    val progress = entryObj["progress"].asIntOrNull() ?: 0
                    val score = entryObj["score"].asDoubleOrNull()
                    val updatedAt = entryObj["updatedAt"].asLongOrNull() ?: 0L
                    val totalEpisodes = mediaObj["episodes"].asIntOrNull()
                    val airingStatus = mediaObj["status"].asStringOrNull()
                    val format = mediaObj["format"].asStringOrNull()

                    AnilistLibraryItem(
                        id = mediaId,
                        title = displayTitle.takeIf { it.isNotBlank() } ?: "Anime $mediaId",
                        posterUrl = posterUrl,
                        progress = progress,
                        totalEpisodes = totalEpisodes,
                        score = score,
                        airingStatus = airingStatus,
                        status = groupStatus,
                        updatedAt = updatedAt,
                        entryId = entryId,
                        format = format,
                    )
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to fetch AniList collection: ${e.message}" }
            emptyList()
        }
    }

    suspend fun fetchMediaReviews(
        mediaId: Int,
        page: Int = 1,
        perPage: Int = 10,
        token: String? = null,
    ): com.nuvio.app.features.anilist.community.AnilistReviewPage {
        val query = """
            query (${'$'}mediaId: Int, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  total
                  currentPage
                  hasNextPage
                }
                reviews(mediaId: ${'$'}mediaId, sort: [RATING_DESC, CREATED_AT_DESC]) {
                  id
                  userId
                  mediaId
                  summary
                  body
                  rating
                  ratingAmount
                  userRating
                  score
                  siteUrl
                  createdAt
                  updatedAt
                  user {
                    id
                    name
                    avatar {
                      large
                      medium
                    }
                    bannerImage
                    donatorBadge
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            put("page", page)
            put("perPage", perPage)
        }

        val root = executeGraphQL(query = query, variables = variables, token = token)
        val pageObj = root?.get("data").asJsonObjectOrNull()?.get("Page").asJsonObjectOrNull()
            ?: return com.nuvio.app.features.anilist.community.AnilistReviewPage()

        val pageInfo = pageObj["pageInfo"].asJsonObjectOrNull()
        val total = pageInfo?.get("total").asIntOrNull() ?: 0
        val hasNextPage = pageInfo?.get("hasNextPage")?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val reviewsArray = pageObj["reviews"].asJsonArrayOrNull().orEmpty()

        val items = reviewsArray.mapNotNull { element ->
            val rObj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            parseReview(rObj)
        }

        return com.nuvio.app.features.anilist.community.AnilistReviewPage(
            items = items,
            page = page,
            hasNextPage = hasNextPage,
            total = total,
        )
    }

    suspend fun saveReview(
        mediaId: Int,
        summary: String,
        body: String,
        score: Int,
        token: String,
        reviewId: Int? = null,
    ): com.nuvio.app.features.anilist.community.AnilistReview? {
        val mutation = """
            mutation (${'$'}mediaId: Int, ${'$'}summary: String, ${'$'}body: String, ${'$'}score: Int, ${'$'}reviewId: Int) {
              SaveReview(mediaId: ${'$'}mediaId, summary: ${'$'}summary, body: ${'$'}body, score: ${'$'}score, id: ${'$'}reviewId) {
                id
                userId
                mediaId
                summary
                body
                rating
                ratingAmount
                userRating
                score
                siteUrl
                createdAt
                updatedAt
                user {
                  id
                  name
                  avatar {
                    large
                    medium
                  }
                  bannerImage
                  donatorBadge
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            put("summary", summary)
            put("body", body)
            put("score", score)
            if (reviewId != null && reviewId > 0) {
                put("reviewId", reviewId)
            }
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        val rObj = root?.get("data").asJsonObjectOrNull()?.get("SaveReview").asJsonObjectOrNull() ?: return null
        return parseReview(rObj)
    }

    suspend fun rateReview(
        reviewId: Int,
        rating: String,
        token: String,
    ): com.nuvio.app.features.anilist.community.AnilistReview? {
        val mutation = """
            mutation (${'$'}reviewId: Int, ${'$'}rating: ReviewRating) {
              RateReview(reviewId: ${'$'}reviewId, rating: ${'$'}rating) {
                id
                rating
                ratingAmount
                userRating
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("reviewId", reviewId)
            put("rating", rating)
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        val rObj = root?.get("data").asJsonObjectOrNull()?.get("RateReview").asJsonObjectOrNull() ?: return null
        val id = rObj["id"].asIntOrNull() ?: reviewId
        val updatedRating = rObj["rating"].asIntOrNull() ?: 0
        val updatedRatingAmount = rObj["ratingAmount"].asIntOrNull() ?: 0
        val updatedUserRating = rObj["userRating"].asStringOrNull()
        return com.nuvio.app.features.anilist.community.AnilistReview(
            id = id,
            rating = updatedRating,
            ratingAmount = updatedRatingAmount,
            userRating = updatedUserRating,
        )
    }

    suspend fun deleteReview(
        reviewId: Int,
        token: String,
    ): Boolean {
        val mutation = """
            mutation (${'$'}id: Int) {
              DeleteReview(id: ${'$'}id) {
                deleted
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("id", reviewId)
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        return root?.get("data").asJsonObjectOrNull()?.get("DeleteReview").asJsonObjectOrNull()
            ?.get("deleted")?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
    }

    private fun parseReview(rObj: JsonObject): com.nuvio.app.features.anilist.community.AnilistReview? {
        val id = rObj["id"].asIntOrNull() ?: return null
        val userId = rObj["userId"].asIntOrNull() ?: 0
        val mediaId = rObj["mediaId"].asIntOrNull() ?: 0
        val summary = rObj["summary"].asStringOrNull().orEmpty()
        val body = rObj["body"].asStringOrNull().orEmpty()
        val rating = rObj["rating"].asIntOrNull() ?: 0
        val ratingAmount = rObj["ratingAmount"].asIntOrNull() ?: 0
        val userRating = rObj["userRating"].asStringOrNull()
        val score = rObj["score"].asIntOrNull() ?: 0
        val siteUrl = rObj["siteUrl"].asStringOrNull()
        val createdAt = rObj["createdAt"].asLongOrNull() ?: 0L
        val updatedAt = rObj["updatedAt"].asLongOrNull() ?: 0L

        val userObj = rObj["user"].asJsonObjectOrNull()
        val user = userObj?.let { u ->
            val uId = u["id"].asIntOrNull() ?: 0
            val uName = u["name"].asStringOrNull().orEmpty()
            val avObj = u["avatar"].asJsonObjectOrNull()
            val avLarge = avObj?.get("large").asStringOrNull()
            val avMedium = avObj?.get("medium").asStringOrNull()
            val banner = u["bannerImage"].asStringOrNull()
            val badge = u["donatorBadge"].asStringOrNull()
            com.nuvio.app.features.anilist.community.AnilistUserSummary(
                id = uId,
                name = uName,
                avatarLarge = avLarge,
                avatarMedium = avMedium,
                bannerImage = banner,
                donatorBadge = badge,
            )
        }

        return com.nuvio.app.features.anilist.community.AnilistReview(
            id = id,
            userId = userId,
            mediaId = mediaId,
            summary = summary,
            body = body,
            rating = rating,
            ratingAmount = ratingAmount,
            userRating = userRating,
            score = score,
            siteUrl = siteUrl,
            createdAt = createdAt,
            updatedAt = updatedAt,
            user = user,
        )
    }

    suspend fun getUserProfile(
        userId: Int? = null,
        username: String? = null,
        token: String? = null,
    ): com.nuvio.app.features.anilist.profile.AnilistFullUserProfile? {
        val query = """
            query (${'$'}id: Int, ${'$'}name: String) {
              User(id: ${'$'}id, name: ${'$'}name) {
                id
                name
                about
                avatar {
                  large
                  medium
                }
                bannerImage
                donatorTier
                donatorBadge
                isFollowing
                isFollower
                statistics {
                  anime {
                    count
                    episodesWatched
                    minutesWatched
                    meanScore
                  }
                }
                favourites {
                  anime {
                    nodes {
                      id
                      title {
                        userPreferred
                        romaji
                        english
                      }
                      coverImage {
                        large
                        medium
                      }
                      format
                      averageScore
                    }
                  }
                  characters {
                    nodes {
                      id
                      name {
                        userPreferred
                        full
                      }
                      image {
                        large
                        medium
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            if (userId != null && userId > 0) put("id", userId)
            if (!username.isNullOrBlank()) put("name", username)
        }

        val root = executeGraphQL(query = query, variables = variables, token = token)
        val userObj = root?.get("data").asJsonObjectOrNull()?.get("User").asJsonObjectOrNull() ?: return null

        val id = userObj["id"].asIntOrNull() ?: return null
        val name = userObj["name"].asStringOrNull().orEmpty()
        val about = userObj["about"].asStringOrNull()
        val avObj = userObj["avatar"].asJsonObjectOrNull()
        val avLarge = avObj?.get("large").asStringOrNull()
        val avMedium = avObj?.get("medium").asStringOrNull()
        val banner = userObj["bannerImage"].asStringOrNull()
        val donatorTier = userObj["donatorTier"].asIntOrNull() ?: 0
        val donatorBadge = userObj["donatorBadge"].asStringOrNull()
        val isFollowing = userObj["isFollowing"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val isFollower = userObj["isFollower"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false

        val statsObj = userObj["statistics"].asJsonObjectOrNull()?.get("anime").asJsonObjectOrNull()
        val animeCount = statsObj?.get("count").asIntOrNull() ?: 0
        val episodesWatched = statsObj?.get("episodesWatched").asIntOrNull() ?: 0
        val minutesWatched = statsObj?.get("minutesWatched").asLongOrNull() ?: 0L
        val daysWatched = (minutesWatched / 1440.0 * 10).roundToInt() / 10.0
        val meanScore = statsObj?.get("meanScore")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0

        val favsObj = userObj["favourites"].asJsonObjectOrNull()
        val favoriteAnime = favsObj?.get("anime").asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull()?.mapNotNull { item ->
            val a = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val aId = a["id"].asIntOrNull() ?: return@mapNotNull null
            val titleObj = a["title"].asJsonObjectOrNull()
            val aTitle = titleObj?.get("userPreferred").asStringOrNull()
                ?: titleObj?.get("romaji").asStringOrNull()
                ?: titleObj?.get("english").asStringOrNull()
                ?: "Anime"
            val covObj = a["coverImage"].asJsonObjectOrNull()
            val aCover = covObj?.get("large").asStringOrNull() ?: covObj?.get("medium").asStringOrNull()
            val aFormat = a["format"].asStringOrNull()
            val aScore = a["averageScore"].asIntOrNull()
            com.nuvio.app.features.anilist.profile.AnilistProfileFavoriteAnime(
                id = aId,
                title = aTitle,
                coverImage = aCover,
                format = aFormat,
                averageScore = aScore,
            )
        } ?: emptyList()

        val favoriteCharacters = favsObj?.get("characters").asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull()?.mapNotNull { item ->
            val c = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val cId = c["id"].asIntOrNull() ?: return@mapNotNull null
            val nameObj = c["name"].asJsonObjectOrNull()
            val cName = nameObj?.get("userPreferred").asStringOrNull()
                ?: nameObj?.get("full").asStringOrNull()
                ?: "Character"
            val imgObj = c["image"].asJsonObjectOrNull()
            val cImage = imgObj?.get("large").asStringOrNull() ?: imgObj?.get("medium").asStringOrNull()
            com.nuvio.app.features.anilist.profile.AnilistProfileFavoriteCharacter(
                id = cId,
                name = cName,
                image = cImage,
            )
        } ?: emptyList()

        return com.nuvio.app.features.anilist.profile.AnilistFullUserProfile(
            id = id,
            name = name,
            about = about,
            avatarLarge = avLarge,
            avatarMedium = avMedium,
            bannerImage = banner,
            donatorTier = donatorTier,
            donatorBadge = donatorBadge,
            isFollowing = isFollowing,
            isFollower = isFollower,
            animeCount = animeCount,
            episodesWatched = episodesWatched,
            minutesWatched = minutesWatched,
            daysWatched = daysWatched,
            meanScore = meanScore,
            favoriteAnime = favoriteAnime,
            favoriteCharacters = favoriteCharacters,
        )
    }

    suspend fun toggleFollowUser(userId: Int, token: String): Boolean {
        val mutation = """
            mutation (${'$'}userId: Int) {
              ToggleFollow(userId: ${'$'}userId) {
                isFollowing
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("userId", userId)
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        return root?.get("data").asJsonObjectOrNull()?.get("ToggleFollow").asJsonObjectOrNull()
            ?.get("isFollowing")?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
    }

    suspend fun getMediaThreads(
        mediaId: Int,
        page: Int = 1,
        token: String? = null,
    ): com.nuvio.app.features.anilist.threads.AnilistThreadPage {
        val query = """
            query (${'$'}mediaId: Int, ${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 20) {
                pageInfo {
                  hasNextPage
                  currentPage
                }
                threads(mediaCategoryId: ${'$'}mediaId, sort: [IS_STICKY, REPLIED_AT_DESC]) {
                  id
                  title
                  body
                  replyCount
                  viewCount
                  isSticky
                  isLocked
                  isLiked
                  likeCount
                  createdAt
                  user {
                    id
                    name
                    avatar {
                      medium
                      large
                    }
                    bannerImage
                    donatorBadge
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            put("page", page)
        }

        val root = executeGraphQL(query = query, variables = variables, token = token)
        val pageObj = root?.get("data").asJsonObjectOrNull()?.get("Page").asJsonObjectOrNull()
        val hasNextPage = pageObj?.get("pageInfo").asJsonObjectOrNull()?.get("hasNextPage")?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val threadsArray = pageObj?.get("threads").asJsonArrayOrNull() ?: return com.nuvio.app.features.anilist.threads.AnilistThreadPage()

        val threads = threadsArray.mapNotNull { item ->
            val t = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val id = t["id"].asIntOrNull() ?: return@mapNotNull null
            val title = t["title"].asStringOrNull().orEmpty()
            val body = t["body"].asStringOrNull().orEmpty()
            val replyCount = t["replyCount"].asIntOrNull() ?: 0
            val viewCount = t["viewCount"].asIntOrNull() ?: 0
            val isSticky = t["isSticky"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
            val isLocked = t["isLocked"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
            val isLiked = t["isLiked"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
            val likeCount = t["likeCount"].asIntOrNull() ?: 0
            val createdAt = t["createdAt"].asLongOrNull() ?: 0L

            val uObj = t["user"].asJsonObjectOrNull()
            val user = uObj?.let { u ->
                val uId = u["id"].asIntOrNull() ?: 0
                val uName = u["name"].asStringOrNull().orEmpty()
                val av = u["avatar"].asJsonObjectOrNull()
                val avMed = av?.get("medium").asStringOrNull()
                val avLg = av?.get("large").asStringOrNull()
                val banner = u["bannerImage"].asStringOrNull()
                val badge = u["donatorBadge"].asStringOrNull()
                com.nuvio.app.features.anilist.community.AnilistUserSummary(
                    id = uId,
                    name = uName,
                    avatarMedium = avMed,
                    avatarLarge = avLg,
                    bannerImage = banner,
                    donatorBadge = badge,
                )
            }

            com.nuvio.app.features.anilist.threads.AnilistThread(
                id = id,
                title = title,
                body = body,
                replyCount = replyCount,
                viewCount = viewCount,
                isSticky = isSticky,
                isLocked = isLocked,
                isLiked = isLiked,
                likeCount = likeCount,
                createdAt = createdAt,
                user = user,
            )
        }

        return com.nuvio.app.features.anilist.threads.AnilistThreadPage(
            threads = threads,
            page = page,
            hasNextPage = hasNextPage,
        )
    }

    suspend fun getThreadComments(
        threadId: Int,
        page: Int = 1,
        token: String? = null,
    ): List<com.nuvio.app.features.anilist.threads.AnilistThreadComment> {
        val query = """
            query (${'$'}threadId: Int, ${'$'}page: Int) {
              Page(page: ${'$'}page, perPage: 30) {
                threadComments(threadId: ${'$'}threadId) {
                  id
                  threadId
                  comment
                  isLiked
                  likeCount
                  createdAt
                  user {
                    id
                    name
                    avatar {
                      medium
                      large
                    }
                    donatorBadge
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("threadId", threadId)
            put("page", page)
        }

        val root = executeGraphQL(query = query, variables = variables, token = token)
        val commentsArray = root?.get("data").asJsonObjectOrNull()
            ?.get("Page").asJsonObjectOrNull()
            ?.get("threadComments").asJsonArrayOrNull() ?: return emptyList()

        return commentsArray.mapNotNull { item ->
            val c = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val id = c["id"].asIntOrNull() ?: return@mapNotNull null
            val tId = c["threadId"].asIntOrNull() ?: threadId
            val comment = c["comment"].asStringOrNull().orEmpty()
            val isLiked = c["isLiked"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
            val likeCount = c["likeCount"].asIntOrNull() ?: 0
            val createdAt = c["createdAt"].asLongOrNull() ?: 0L

            val uObj = c["user"].asJsonObjectOrNull()
            val user = uObj?.let { u ->
                val uId = u["id"].asIntOrNull() ?: 0
                val uName = u["name"].asStringOrNull().orEmpty()
                val av = u["avatar"].asJsonObjectOrNull()
                val avMed = av?.get("medium").asStringOrNull()
                val avLg = av?.get("large").asStringOrNull()
                val badge = u["donatorBadge"].asStringOrNull()
                com.nuvio.app.features.anilist.community.AnilistUserSummary(
                    id = uId,
                    name = uName,
                    avatarMedium = avMed,
                    avatarLarge = avLg,
                    donatorBadge = badge,
                )
            }

            com.nuvio.app.features.anilist.threads.AnilistThreadComment(
                id = id,
                threadId = tId,
                comment = comment,
                isLiked = isLiked,
                likeCount = likeCount,
                createdAt = createdAt,
                user = user,
            )
        }
    }

    suspend fun saveThreadComment(
        threadId: Int,
        comment: String,
        token: String,
    ): com.nuvio.app.features.anilist.threads.AnilistThreadComment? {
        val mutation = """
            mutation (${'$'}threadId: Int, ${'$'}comment: String) {
              SaveThreadComment(threadId: ${'$'}threadId, comment: ${'$'}comment) {
                id
                threadId
                comment
                likeCount
                createdAt
                user {
                  id
                  name
                  avatar {
                    medium
                  }
                  donatorBadge
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("threadId", threadId)
            put("comment", comment)
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        val c = root?.get("data").asJsonObjectOrNull()?.get("SaveThreadComment").asJsonObjectOrNull() ?: return null
        val id = c["id"].asIntOrNull() ?: return null
        val tId = c["threadId"].asIntOrNull() ?: threadId
        val commentText = c["comment"].asStringOrNull().orEmpty()
        val likeCount = c["likeCount"].asIntOrNull() ?: 0
        val createdAt = c["createdAt"].asLongOrNull() ?: (io.ktor.util.date.GMTDate().timestamp / 1000)

        val uObj = c["user"].asJsonObjectOrNull()
        val user = uObj?.let { u ->
            val uId = u["id"].asIntOrNull() ?: 0
            val uName = u["name"].asStringOrNull().orEmpty()
            val av = u["avatar"].asJsonObjectOrNull()
            val avMed = av?.get("medium").asStringOrNull()
            val badge = u["donatorBadge"].asStringOrNull()
            com.nuvio.app.features.anilist.community.AnilistUserSummary(
                id = uId,
                name = uName,
                avatarMedium = avMed,
                donatorBadge = badge,
            )
        }

        return com.nuvio.app.features.anilist.threads.AnilistThreadComment(
            id = id,
            threadId = tId,
            comment = commentText,
            isLiked = false,
            likeCount = likeCount,
            createdAt = createdAt,
            user = user,
        )
    }

    suspend fun toggleLikeV2(
        id: Int,
        type: String, // "THREAD", "THREAD_COMMENT"
        token: String,
    ): Boolean {
        val mutation = """
            mutation (${'$'}id: Int, ${'$'}type: LikeableType) {
              ToggleLikeV2(id: ${'$'}id, type: ${'$'}type) {
                ... on Thread {
                  id
                  isLiked
                  likeCount
                }
                ... on ThreadComment {
                  id
                  isLiked
                  likeCount
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("id", id)
            put("type", type)
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        return root?.get("data") != null
    }

    suspend fun getMediaRecommendations(
        mediaId: Int,
        page: Int = 1,
        token: String? = null,
    ): com.nuvio.app.features.anilist.recommendations.AnilistRecommendationPage {
        val query = """
            query (${'$'}mediaId: Int, ${'$'}page: Int) {
              Media(id: ${'$'}mediaId) {
                recommendations(sort: [RATING_DESC], page: ${'$'}page, perPage: 20) {
                  pageInfo {
                    hasNextPage
                    currentPage
                  }
                  nodes {
                    id
                    rating
                    userRating
                    mediaRecommendation {
                      id
                      title {
                        userPreferred
                        romaji
                        english
                      }
                      coverImage {
                        large
                        medium
                      }
                      averageScore
                      format
                      status
                      episodes
                    }
                    user {
                      id
                      name
                      avatar {
                        medium
                      }
                      donatorBadge
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            put("page", page)
        }

        val root = executeGraphQL(query = query, variables = variables, token = token)
        val recObj = root?.get("data").asJsonObjectOrNull()
            ?.get("Media").asJsonObjectOrNull()
            ?.get("recommendations").asJsonObjectOrNull() ?: return com.nuvio.app.features.anilist.recommendations.AnilistRecommendationPage()

        val hasNextPage = recObj["pageInfo"].asJsonObjectOrNull()?.get("hasNextPage")?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val nodes = recObj["nodes"].asJsonArrayOrNull() ?: return com.nuvio.app.features.anilist.recommendations.AnilistRecommendationPage()

        val list = nodes.mapNotNull { item ->
            val node = item.asJsonObjectOrNull() ?: return@mapNotNull null
            val id = node["id"].asIntOrNull() ?: return@mapNotNull null
            val rating = node["rating"].asIntOrNull() ?: 0
            val userRating = node["userRating"].asStringOrNull()

            val mediaRec = node["mediaRecommendation"].asJsonObjectOrNull() ?: return@mapNotNull null
            val mId = mediaRec["id"].asIntOrNull() ?: return@mapNotNull null
            val titleObj = mediaRec["title"].asJsonObjectOrNull()
            val mTitle = titleObj?.get("userPreferred").asStringOrNull()
                ?: titleObj?.get("romaji").asStringOrNull()
                ?: titleObj?.get("english").asStringOrNull()
                ?: "Anime"
            val covObj = mediaRec["coverImage"].asJsonObjectOrNull()
            val mCover = covObj?.get("large").asStringOrNull() ?: covObj?.get("medium").asStringOrNull()
            val avgScore = mediaRec["averageScore"].asIntOrNull()
            val format = mediaRec["format"].asStringOrNull()
            val status = mediaRec["status"].asStringOrNull()
            val episodes = mediaRec["episodes"].asIntOrNull()

            val uObj = node["user"].asJsonObjectOrNull()
            val user = uObj?.let { u ->
                val uId = u["id"].asIntOrNull() ?: 0
                val uName = u["name"].asStringOrNull().orEmpty()
                val av = u["avatar"].asJsonObjectOrNull()
                val avMed = av?.get("medium").asStringOrNull()
                val badge = u["donatorBadge"].asStringOrNull()
                com.nuvio.app.features.anilist.community.AnilistUserSummary(
                    id = uId,
                    name = uName,
                    avatarMedium = avMed,
                    donatorBadge = badge,
                )
            }

            com.nuvio.app.features.anilist.recommendations.AnilistRecommendation(
                id = id,
                rating = rating,
                userRating = userRating,
                mediaId = mId,
                title = mTitle,
                coverImage = mCover,
                averageScore = avgScore,
                format = format,
                status = status,
                episodes = episodes,
                user = user,
            )
        }

        return com.nuvio.app.features.anilist.recommendations.AnilistRecommendationPage(
            recommendations = list,
            page = page,
            hasNextPage = hasNextPage,
        )
    }

    suspend fun saveRecommendationVote(
        mediaId: Int,
        mediaRecommendationId: Int,
        rating: String, // "RATE_UP", "RATE_DOWN", "NO_RATING"
        token: String,
    ): Boolean {
        val mutation = """
            mutation (${'$'}mediaId: Int, ${'$'}mediaRecommendationId: Int, ${'$'}rating: RecommendationRating) {
              SaveRecommendation(mediaId: ${'$'}mediaId, mediaRecommendationId: ${'$'}mediaRecommendationId, rating: ${'$'}rating) {
                id
                rating
                userRating
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("mediaId", mediaId)
            put("mediaRecommendationId", mediaRecommendationId)
            put("rating", rating)
        }

        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        return root?.get("data") != null
    }
}

