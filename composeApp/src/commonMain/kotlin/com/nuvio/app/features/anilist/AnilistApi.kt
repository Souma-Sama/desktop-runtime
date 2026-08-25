package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
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
    private val log = Logger.withTag("AnilistApi")
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
            "User-Agent" to "Nuvio-Kai/1.0",
        )
        if (!token.isNullOrBlank()) {
            val sanitized = token.trim().removePrefix("Bearer ").trim()
            headers["Authorization"] = "Bearer $sanitized"
        }

        val responseText = try {
            httpPostJsonWithHeaders(
                url = GRAPHQL_ENDPOINT,
                body = payload,
                headers = headers,
            )
        } catch (e: Exception) {
            log.w(e) { "executeGraphQL request failed: ${e.message}" }
            null
        } ?: return null

        if (responseText.isBlank()) {
            return null
        }

        return try {
            json.parseToJsonElement(responseText).jsonObject
        } catch (e: Exception) {
            log.w(e) { "Failed to parse GraphQL response JSON: $responseText" }
            null
        }
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

    suspend fun resolveArmAnilistId(source: String, id: String): Int? {
        val url = "https://arm.haglund.dev/api/v2/ids?source=$source&id=$id&include=anilist"
        return runCatching {
            val responseText = com.nuvio.app.features.addons.httpGetText(url)
            val jsonElement = json.parseToJsonElement(responseText)
            jsonElement.jsonObject["anilist"]?.jsonPrimitive?.intOrNull
                ?: jsonElement.jsonObject["anilist"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        }.getOrNull()
    }

    suspend fun searchViaKitsu(query: String): Int? {
        val safeQuery = query.trim().replace(" ", "%20")
        val rawUrl = "https://kitsu.io/api/edge/anime?filter%5Btext%5D=$safeQuery&page%5Blimit%5D=1"
        val url = rawUrl.encodeUnsafeHttpUrlCharacters()
        return runCatching {
            val text = com.nuvio.app.features.addons.httpGetText(url)
            val jsonElement = json.parseToJsonElement(text)
            val dataArray = jsonElement.jsonObject["data"]?.jsonArray
            val firstItem = dataArray?.firstOrNull()?.jsonObject
            val kitsuId = firstItem?.get("id")?.jsonPrimitive?.contentOrNull
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
        val entryObj = root?.get("data")?.jsonObject?.get("SaveMediaListEntry")?.jsonObject ?: return null
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
        return root?.get("data")?.jsonObject?.get("DeleteMediaListEntry")?.jsonObject?.get("deleted")?.jsonPrimitive?.contentOrNull == "true"
    }

    private fun parseMedia(obj: JsonObject): AnilistMedia? = runCatching {
        val id = obj["id"]?.jsonPrimitive?.intOrNull
            ?: obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?: return null
        val idMal = obj["idMal"]?.jsonPrimitive?.intOrNull
            ?: obj["idMal"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val format = obj["format"]?.jsonPrimitive?.contentOrNull
        val status = obj["status"]?.jsonPrimitive?.contentOrNull
        val episodes = obj["episodes"]?.jsonPrimitive?.intOrNull
            ?: obj["episodes"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val duration = obj["duration"]?.jsonPrimitive?.intOrNull
            ?: obj["duration"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val bannerImage = obj["bannerImage"]?.jsonPrimitive?.contentOrNull
        val averageScore = obj["averageScore"]?.jsonPrimitive?.intOrNull
            ?: obj["averageScore"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
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
                ?: it["episode"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            val airAt = it["airingAt"]?.jsonPrimitive?.longOrNull
                ?: it["airingAt"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            val timeUntil = it["timeUntilAiring"]?.jsonPrimitive?.longOrNull
                ?: it["timeUntilAiring"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
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
        val id = obj["id"]?.jsonPrimitive?.intOrNull
            ?: obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val mediaId = obj["mediaId"]?.jsonPrimitive?.intOrNull
            ?: obj["mediaId"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: defaultMediaId
        val statusStr = obj["status"]?.jsonPrimitive?.contentOrNull
        val score = obj["score"]?.jsonPrimitive?.doubleOrNull
            ?: obj["score"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
        val progress = obj["progress"]?.jsonPrimitive?.intOrNull
            ?: obj["progress"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val repeat = obj["repeat"]?.jsonPrimitive?.intOrNull
            ?: obj["repeat"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val updatedAt = obj["updatedAt"]?.jsonPrimitive?.longOrNull
            ?: obj["updatedAt"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L

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
}
