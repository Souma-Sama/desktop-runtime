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
        if (this is JsonPrimitive && this !is kotlinx.serialization.json.JsonNull) this.contentOrNull else null

    private fun JsonElement?.asIntOrNull(): Int? =
        if (this is JsonPrimitive && this !is kotlinx.serialization.json.JsonNull) (this.intOrNull ?: this.contentOrNull?.toIntOrNull()) else null

    private fun JsonElement?.asDoubleOrNull(): Double? =
        if (this is JsonPrimitive && this !is kotlinx.serialization.json.JsonNull) (this.doubleOrNull ?: this.contentOrNull?.toDoubleOrNull()) else null

    private fun JsonElement?.asLongOrNull(): Long? =
        if (this is JsonPrimitive && this !is kotlinx.serialization.json.JsonNull) (this.longOrNull ?: this.contentOrNull?.toLongOrNull()) else null

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
            .asJsonObjectOrNull()?.get("Page")
            .asJsonObjectOrNull()?.get("media")
            .asJsonArrayOrNull() ?: return emptyList()

        return mediaList.mapNotNull { element ->
            element.asJsonObjectOrNull()?.let { parseMedia(it) }
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
        val description = obj["description"].asStringOrNull()

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

        val entryObj = obj["mediaListEntry"].asJsonObjectOrNull()
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
}
