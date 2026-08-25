package com.nuvio.app.features.anilist

import com.nuvio.app.features.addons.httpRequestRaw
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
    private const val GRAPHQL_ENDPOINT = "https://graphql.anilist.co"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "User-Agent" to "Nuvio-Kai/1.0",
        )
        if (!token.isNullOrBlank()) {
            val sanitized = token.trim().removePrefix("Bearer ").trim()
            headers["Authorization"] = "Bearer $sanitized"
        }

        val response = runCatching {
            httpRequestRaw(
                method = "POST",
                url = GRAPHQL_ENDPOINT,
                headers = headers,
                body = payload,
            )
        }.getOrNull() ?: return null

        if (response.status !in 200..299 || response.body.isBlank()) {
            return null
        }

        return runCatching {
            json.parseToJsonElement(response.body).jsonObject
        }.getOrNull()
    }

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
        val viewer = root["data"]?.jsonObject?.get("Viewer")?.jsonObject ?: return null
        val id = viewer["id"]?.jsonPrimitive?.intOrNull ?: return null
        val name = viewer["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val avatar = viewer["avatar"]?.jsonObject
        val avatarUrl = avatar?.get("large")?.jsonPrimitive?.contentOrNull
            ?: avatar?.get("medium")?.jsonPrimitive?.contentOrNull

        return AnilistUser(
            id = id,
            name = name,
            avatarUrl = avatarUrl,
        )
    }

    suspend fun searchAnime(
        query: String,
        token: String? = null,
    ): List<AnilistMedia> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val graphQLQuery = """
            query (${'$'}search: String) {
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
            ?.jsonObject?.get("Page")
            ?.jsonObject?.get("media")
            ?.jsonArray ?: return emptyList()

        return mediaList.mapNotNull { parseMedia(it.jsonObject) }
    }

    suspend fun fetchMediaByMalId(
        malId: Int,
        token: String? = null,
    ): AnilistMedia? {
        val query = """
            query (${'$'}idMal: Int) {
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
        val mediaObj = root?.get("data")?.jsonObject?.get("Media")?.jsonObject ?: return null
        return parseMedia(mediaObj)
    }

    suspend fun fetchMediaById(
        mediaId: Int,
        token: String? = null,
    ): AnilistMedia? {
        val query = """
            query (${'$'}id: Int) {
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

        val variables = buildJsonObject { put("id", mediaId) }
        val root = executeGraphQL(query = query, variables = variables, token = token)
        val mediaObj = root?.get("data")?.jsonObject?.get("Media")?.jsonObject ?: return null
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
            mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int, ${'$'}score: Float) {
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
        val entryObj = root?.get("data")?.jsonObject?.get("SaveMediaListEntry")?.jsonObject ?: return null
        return parseMediaListEntry(entryObj, defaultMediaId = mediaId)
    }

    suspend fun deleteMediaListEntry(
        entryId: Int,
        token: String,
    ): Boolean {
        val mutation = """
            mutation (${'$'}id: Int) {
              DeleteMediaListEntry(id: ${'$'}id) {
                deleted
              }
            }
        """.trimIndent()

        val variables = buildJsonObject { put("id", entryId) }
        val root = executeGraphQL(query = mutation, variables = variables, token = token)
        return root?.get("data")?.jsonObject?.get("DeleteMediaListEntry")?.jsonObject?.get("deleted")?.jsonPrimitive?.contentOrNull == "true"
    }

    private fun parseMedia(obj: JsonObject): AnilistMedia? = runCatching {
        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return null
        val idMal = obj["idMal"]?.jsonPrimitive?.intOrNull
        val format = obj["format"]?.jsonPrimitive?.contentOrNull
        val status = obj["status"]?.jsonPrimitive?.contentOrNull
        val episodes = obj["episodes"]?.jsonPrimitive?.intOrNull
        val duration = obj["duration"]?.jsonPrimitive?.intOrNull
        val bannerImage = obj["bannerImage"]?.jsonPrimitive?.contentOrNull
        val averageScore = obj["averageScore"]?.jsonPrimitive?.intOrNull
        val description = obj["description"]?.jsonPrimitive?.contentOrNull

        val titleObj = obj["title"]?.jsonObject
        val title = titleObj?.let {
            AnilistTitle(
                romaji = it["romaji"]?.jsonPrimitive?.contentOrNull,
                english = it["english"]?.jsonPrimitive?.contentOrNull,
                native = it["native"]?.jsonPrimitive?.contentOrNull,
            )
        }

        val coverObj = obj["coverImage"]?.jsonObject
        val coverImage = coverObj?.let {
            AnilistCoverImage(
                extraLarge = it["extraLarge"]?.jsonPrimitive?.contentOrNull,
                large = it["large"]?.jsonPrimitive?.contentOrNull,
                medium = it["medium"]?.jsonPrimitive?.contentOrNull,
            )
        }

        val genres = obj["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

        val nextObj = obj["nextAiringEpisode"]?.jsonObject
        val nextAiringEpisode = nextObj?.let {
            val ep = it["episode"]?.jsonPrimitive?.intOrNull
            val airAt = it["airingAt"]?.jsonPrimitive?.longOrNull
            val timeUntil = it["timeUntilAiring"]?.jsonPrimitive?.longOrNull
            if (ep != null && airAt != null && timeUntil != null) {
                AnilistNextAiringEpisode(ep, airAt, timeUntil)
            } else null
        }

        val entryObj = obj["mediaListEntry"]?.jsonObject
        val mediaListEntry = entryObj?.let { parseMediaListEntry(it, defaultMediaId = id) }

        AnilistMedia(
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
            mediaListEntry = mediaListEntry,
        )
    }.getOrNull()

    private fun parseMediaListEntry(obj: JsonObject, defaultMediaId: Int): AnilistMediaListEntry = runCatching {
        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0
        val mediaId = obj["mediaId"]?.jsonPrimitive?.intOrNull ?: defaultMediaId
        val statusStr = obj["status"]?.jsonPrimitive?.contentOrNull
        val score = obj["score"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val progress = obj["progress"]?.jsonPrimitive?.intOrNull ?: 0
        val repeat = obj["repeat"]?.jsonPrimitive?.intOrNull ?: 0
        val updatedAt = obj["updatedAt"]?.jsonPrimitive?.longOrNull ?: 0L

        AnilistMediaListEntry(
            id = id,
            mediaId = mediaId,
            status = AnilistMediaListStatus.fromString(statusStr),
            score = score,
            progress = progress,
            repeat = repeat,
            updatedAt = updatedAt,
        )
    }.getOrElse { AnilistMediaListEntry(mediaId = defaultMediaId) }
}
