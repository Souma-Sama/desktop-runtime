package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
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
              Page(page: 1, perPage: 10) {
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

        val graphQLQuery = """
            query (${'$'}id: Int, ${'$'}search: String) {
              Staff(id: ${'$'}id, search: ${'$'}search) {
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
              Character(id: ${'$'}id, search: ${'$'}search) {
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

        val variables = buildJsonObject {
            if (staffId != null && staffId > 0) put("id", staffId)
            if (cleanSearch != null) put("search", cleanSearch)
        }

        val root = executeGraphQL(query = graphQLQuery, variables = variables, token = null) ?: return null
        val dataObj = root.get("data").asJsonObjectOrNull() ?: return null
        val staffObj = dataObj["Staff"].asJsonObjectOrNull()
        val charObj = dataObj["Character"].asJsonObjectOrNull()

        val entityObj = staffObj ?: charObj ?: return null
        val isStaff = staffObj != null

        val id = entityObj["id"].asIntOrNull() ?: staffId ?: 0
        val nameObj = entityObj["name"].asJsonObjectOrNull()
        val fullName = nameObj?.get("full").asStringOrNull() ?: cleanSearch.orEmpty()
        val photo = entityObj["image"].asJsonObjectOrNull()?.get("large").asStringOrNull()
        val bio = entityObj["description"].asStringOrNull()

        val dobObj = entityObj["dateOfBirth"].asJsonObjectOrNull()
        val dobYear = dobObj?.get("year").asIntOrNull()
        val dobMonth = dobObj?.get("month").asIntOrNull()
        val dobDay = dobObj?.get("day").asIntOrNull()
        val birthday = if (dobYear != null) {
            val m = (dobMonth ?: 1).toString().padStart(2, '0')
            val d = (dobDay ?: 1).toString().padStart(2, '0')
            "$dobYear-$m-$d"
        } else null

        val hometown = entityObj["homeTown"].asStringOrNull() ?: "Japan"

        val rawNodes = (if (isStaff) {
            staffObj?.get("characterMedia").asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull()
        } else {
            charObj?.get("media").asJsonObjectOrNull()?.get("nodes").asJsonArrayOrNull()
        }) ?: JsonArray(emptyList())

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
            knownFor = if (isStaff) "Voice Acting" else "Character",
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

    private val mediaCache = mutableMapOf<Int, AnilistMedia>()

    fun getCachedMedia(mediaId: Int): AnilistMedia? = mediaCache[mediaId]

    suspend fun fetchMediaById(
        mediaId: Int,
        token: String? = null,
    ): AnilistMedia? {
        val cached = mediaCache[mediaId]
        if (cached != null && cached.characters.isNotEmpty() && cached.recommendations.isNotEmpty() && (!token.isNullOrBlank() == (cached.mediaListEntry != null) || token.isNullOrBlank())) {
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
              Media(id: ${'$'}id, type: ANIME) {
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
                characters(sort: RELEVANCE, perPage: 25) {
                  edges {
                    node {
                      id
                      name {
                        full
                      }
                      image {
                        large
                      }
                    }
                    voiceActors(language: JAPANESE) {
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
                }
                endDate {
                  year
                }
                staff(perPage: 15) {
                  edges {
                    role
                    node {
                      name {
                        full
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
        return parseMedia(mediaObj)
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

    private fun parseMedia(obj: JsonObject): AnilistMedia? = runCatching {
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
                val node = edge["node"].asJsonObjectOrNull()
                val charId = node?.get("id").asIntOrNull()
                val charName = node?.get("name").asJsonObjectOrNull()?.get("full").asStringOrNull()
                val charImg = node?.get("image").asJsonObjectOrNull()?.get("large").asStringOrNull()

                val vaNode = edge["voiceActors"].asJsonArrayOrNull()?.firstOrNull().asJsonObjectOrNull()
                val vaId = vaNode?.get("id").asIntOrNull()
                val vaName = vaNode?.get("name").asJsonObjectOrNull()?.get("full").asStringOrNull()
                val vaImg = vaNode?.get("image").asJsonObjectOrNull()?.get("large").asStringOrNull()

                val va = if (vaName != null) AnilistCharacterVoiceActor(id = vaId, name = vaName, image = vaImg) else null
                if (charName != null || vaName != null) {
                    AnilistCharacter(id = charId, name = charName, image = charImg, voiceActor = va)
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
                val sName = sObj["node"].asJsonObjectOrNull()?.get("name").asJsonObjectOrNull()?.get("full").asStringOrNull()
                if (sName != null) AnilistStaff(name = sName, role = sRole) else null
            }.orEmpty()

        val startYear = obj["startDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()
        val endYear = obj["endDate"].asJsonObjectOrNull()?.get("year").asIntOrNull()

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
            endDateYear = endYear,
            mediaListEntry = mediaListEntry,
            relations = relations,
        )
        mediaCache[id] = media
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
            val nestedRelations = parseRelations(node)
            AnilistRelation(
                relationType = relType,
                id = nId,
                title = nTitle,
                format = node["format"].asStringOrNull(),
                episodes = node["episodes"].asIntOrNull(),
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
}
