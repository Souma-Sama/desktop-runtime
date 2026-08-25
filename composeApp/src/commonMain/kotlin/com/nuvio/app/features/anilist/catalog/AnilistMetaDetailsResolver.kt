package com.nuvio.app.features.anilist.catalog

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistMedia
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.details.MetaVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object AnilistMetaDetailsResolver {
    private val log = Logger.withTag("AnilistMetaResolver")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = if (this is JsonObject) this else null
    private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = if (this is JsonArray) this else null
    private fun JsonElement?.asStringOrNull(): String? = if (this is JsonPrimitive && this !is JsonNull) this.contentOrNull else null
    private fun JsonElement?.asIntOrNull(): Int? = if (this is JsonPrimitive && this !is JsonNull) (this.intOrNull ?: this.contentOrNull?.toIntOrNull()) else null

    suspend fun resolveMetaDetails(rawId: String): MetaDetails? = withContext(Dispatchers.Default) {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId) ?: return@withContext null
        val token = AnilistAuthRepository.token.value

        log.d { "resolveMetaDetails: rawId=$rawId, anilistId=$anilistId" }

        // 1. Fetch base AniList media details
        val media: AnilistMedia = AnilistApi.fetchMediaById(anilistId, token = token) ?: return@withContext null

        // 2. Resolve external IDs (IMDb ID & Kitsu ID) via ARM & Kitsu APIs
        val armImdbId = resolveArmImdbId(anilistId)
        val kitsuId = resolveKitsuId(anilistId, media.title?.displayTitle.orEmpty())

        // 3. 1080p Backdrops & Clear PNG Logos from Metahub
        val backdrop = if (!armImdbId.isNullOrBlank()) {
            "https://images.metahub.space/background/medium/$armImdbId/img"
        } else {
            media.bannerImage ?: media.coverImage?.extraLarge
        }

        val logo = if (!armImdbId.isNullOrBlank()) {
            "https://images.metahub.space/logo/medium/$armImdbId/img"
        } else null

        val poster = media.coverImage?.extraLarge
            ?: media.coverImage?.large
            ?: media.coverImage?.medium

        // 4. Fetch Episode Data from Kitsu
        val episodeMap = if (!kitsuId.isNullOrBlank()) {
            fetchKitsuEpisodes(kitsuId)
        } else emptyMap()

        val isMovie = media.format == "MOVIE" || (media.episodes == 1 && media.format != "TV")
        val contentType = if (isMovie) "movie" else "series"
        val totalEpisodes = media.episodes ?: episodeMap.size.takeIf { it > 0 } ?: 12

        // 5. If IMDb ID is resolved, load base Cinemeta details (Trailers, Backdrops, Logos, Episodes)
        var cinemetaMeta: MetaDetails? = null
        if (!armImdbId.isNullOrBlank()) {
            val cinemetaUrl = "https://v3-cinemeta.strem.io/meta/$contentType/$armImdbId.json"
            val cinemetaResponse = runCatching { httpGetText(cinemetaUrl) }.getOrNull()
            if (!cinemetaResponse.isNullOrBlank()) {
                cinemetaMeta = runCatching { com.nuvio.app.features.details.MetaDetailsParser.parse(cinemetaResponse) }.getOrNull()
            }
        }

        val mappedVideos = if (isMovie) {
            emptyList()
        } else {
            (1..totalEpisodes).map { epNum ->
                val cinemetaEp = cinemetaMeta?.videos?.firstOrNull { it.episode == epNum }
                val epData = episodeMap[epNum]
                val epTitle = cinemetaEp?.title?.takeIf { it.isNotBlank() } ?: epData?.title?.takeIf { it.isNotBlank() } ?: "Episode $epNum"
                val epOverview = cinemetaEp?.overview?.takeIf { it.isNotBlank() } ?: epData?.overview
                val epThumbnail = cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: epData?.thumbnail
                    ?: if (!armImdbId.isNullOrBlank()) "https://images.metahub.space/screenshot/medium/$armImdbId/1/$epNum/img" else null

                val videoId = when {
                    !kitsuId.isNullOrBlank() -> "kitsu:$kitsuId:$epNum"
                    !armImdbId.isNullOrBlank() -> "$armImdbId:1:$epNum"
                    else -> "anilist:$anilistId:$epNum"
                }

                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = 1,
                    episode = epNum,
                    overview = epOverview,
                    thumbnail = epThumbnail,
                )
            }
        }

        val formattedScore = if (media.averageScore != null && media.averageScore > 0) {
            val score = media.averageScore / 10.0
            "${(score * 10).toInt() / 10.0}"
        } else null

        if (cinemetaMeta != null) {
            return@withContext cinemetaMeta.copy(
                id = "ani_$anilistId",
                type = if (isMovie) "movie" else "series",
                name = media.title?.displayTitle ?: cinemetaMeta.name,
                poster = media.coverImage?.extraLarge ?: cinemetaMeta.poster,
                background = cinemetaMeta.background ?: backdrop,
                logo = cinemetaMeta.logo ?: logo,
                description = media.description ?: cinemetaMeta.description,
                videos = mappedVideos,
                defaultVideoId = if (isMovie) {
                    when {
                        !kitsuId.isNullOrBlank() -> "kitsu:$kitsuId"
                        else -> cinemetaMeta.defaultVideoId ?: armImdbId
                    }
                } else null,
            )
        }

        // Fallback when Cinemeta / IMDb ID is unavailable: build from AniList + Kitsu
        MetaDetails(
            id = "ani_$anilistId",
            type = if (isMovie) "movie" else "series",
            name = media.title?.displayTitle.orEmpty(),
            poster = poster,
            background = backdrop,
            logo = logo,
            description = media.description,
            releaseInfo = if (media.episodes != null) "${media.episodes} Episodes" else null,
            status = media.status,
            imdbRating = formattedScore,
            genres = media.genres,
            country = "JP",
            language = "ja",
            hasScheduledVideos = media.status == "RELEASING",
            defaultVideoId = if (isMovie) {
                when {
                    !kitsuId.isNullOrBlank() -> "kitsu:$kitsuId"
                    !armImdbId.isNullOrBlank() -> armImdbId
                    else -> "anilist:$anilistId"
                }
            } else null,
            videos = mappedVideos,
        )
    }

    suspend fun resolveArmImdbId(anilistId: Int): String? = runCatching {
        val url = "https://arm.haglund.dev/api/v2/ids?source=anilist&id=$anilistId&include=imdb"
        val response = httpGetText(url)
        val jsonElement = json.parseToJsonElement(response)
        jsonElement.asJsonObjectOrNull()?.get("imdb").asStringOrNull()
    }.getOrNull()

    private suspend fun resolveKitsuId(anilistId: Int, fallbackTitle: String): String? = runCatching {
        // 1. Try ARM Anilist -> Kitsu ID
        val url = "https://arm.haglund.dev/api/v2/ids?source=anilist&id=$anilistId&include=kitsu"
        val response = httpGetText(url)
        val jsonElement = json.parseToJsonElement(response)
        val armKitsuId = jsonElement.asJsonObjectOrNull()?.get("kitsu").asStringOrNull()
            ?: jsonElement.asJsonObjectOrNull()?.get("kitsu").asIntOrNull()?.toString()
        if (!armKitsuId.isNullOrBlank()) return@runCatching armKitsuId

        // 2. Fallback: Search Kitsu by Title
        if (fallbackTitle.isNotBlank()) {
            val safeTitle = fallbackTitle.trim().replace(" ", "%20")
            val searchUrl = "https://kitsu.io/api/edge/anime?filter%5Btext%5D=$safeTitle&page%5Blimit%5D=1"
            val searchResponse = httpGetText(searchUrl)
            val searchJson = json.parseToJsonElement(searchResponse)
            searchJson.asJsonObjectOrNull()?.get("data").asJsonArrayOrNull()
                ?.firstOrNull().asJsonObjectOrNull()
                ?.get("id").asStringOrNull()
        } else null
    }.getOrNull()

    private data class KitsuEpisodeData(
        val title: String?,
        val overview: String?,
        val thumbnail: String?,
    )

    private suspend fun fetchKitsuEpisodes(kitsuId: String): Map<Int, KitsuEpisodeData> = runCatching {
        val cleanKitsuId = kitsuId.removePrefix("kitsu:")
        val url = "https://kitsu.io/api/edge/anime/$cleanKitsuId/episodes?page[limit]=50"
        val response = httpGetText(url)
        val root = json.parseToJsonElement(response)
        val dataArray = root.asJsonObjectOrNull()?.get("data").asJsonArrayOrNull() ?: return@runCatching emptyMap()

        val result = mutableMapOf<Int, KitsuEpisodeData>()
        dataArray.forEach { item ->
            val itemObj = item.asJsonObjectOrNull() ?: return@forEach
            val attrs = itemObj["attributes"].asJsonObjectOrNull() ?: return@forEach
            val epNum = attrs["number"].asIntOrNull() ?: attrs["relativeNumber"].asIntOrNull() ?: return@forEach

            val titlesObj = attrs["titles"].asJsonObjectOrNull()
            val epTitle = titlesObj?.get("en_us").asStringOrNull()
                ?: titlesObj?.get("en_jp").asStringOrNull()
                ?: titlesObj?.get("canonical").asStringOrNull()
                ?: attrs["canonicalTitle"].asStringOrNull()

            val overview = attrs["synopsis"].asStringOrNull() ?: attrs["description"].asStringOrNull()
            val thumbnailObj = attrs["thumbnail"].asJsonObjectOrNull()
            val thumbnail = thumbnailObj?.get("original").asStringOrNull()
                ?: thumbnailObj?.get("medium").asStringOrNull()
                ?: thumbnailObj?.get("large").asStringOrNull()

            result[epNum] = KitsuEpisodeData(
                title = epTitle,
                overview = overview,
                thumbnail = thumbnail,
            )
        }
        result
    }.getOrElse { emptyMap() }
}
