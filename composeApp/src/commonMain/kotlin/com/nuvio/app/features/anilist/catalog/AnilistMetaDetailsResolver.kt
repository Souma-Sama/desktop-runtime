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
        val media: AnilistMedia = AnilistApi.getCachedMedia(anilistId)
            ?: AnilistApi.fetchMediaById(anilistId, token = token)
            ?: return@withContext null

        // 2. Resolve external IDs (IMDb ID & Kitsu ID) via ARM & Kitsu APIs
        val armMapping = resolveArmMapping(anilistId)
        val armImdbId = armMapping.imdbId
        val targetSeason = armMapping.season
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

        val cleanDescription = cinemetaMeta?.description?.takeIf { it.isNotBlank() }
            ?: media.description?.cleanAnilistDescription()

        val castPersons = media.characters.mapNotNull { char ->
            val va = char.voiceActor
            val name = va?.name ?: char.name ?: return@mapNotNull null
            val characterRole = if (char.name != null && va?.name != null) "${char.name} (VA)" else char.name ?: "Cast"
            val photo = va?.image ?: char.image
            com.nuvio.app.features.details.MetaPerson(
                name = name,
                role = characterRole,
                photo = photo,
                tmdbId = null,
            )
        }

        val animationStudios = media.studios.filter { it.isAnimationStudio }.map { studio ->
            com.nuvio.app.features.details.MetaCompany(
                name = studio.name ?: "Studio",
                tmdbId = null,
            )
        }

        val networks = media.studios.filterNot { it.isAnimationStudio }.map { studio ->
            com.nuvio.app.features.details.MetaCompany(
                name = studio.name ?: "Network",
                tmdbId = null,
            )
        }

        val recommendations = media.recommendations.mapNotNull { rec ->
            val recId = rec.id
            val isRecMovie = rec.format == "MOVIE" || rec.episodes == 1
            val recType = if (isRecMovie) "movie" else "series"
            val recPoster = rec.coverImage?.extraLarge ?: rec.coverImage?.large ?: return@mapNotNull null
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$recId",
                type = recType,
                name = rec.title?.displayTitle.orEmpty(),
                poster = recPoster,
                banner = rec.bannerImage,
                logo = null,
                description = null,
                releaseInfo = if (rec.episodes != null) "${rec.episodes} Ep" else null,
            )
        }

        val trailers = if (media.trailer != null && media.trailer.id != null) {
            listOf(
                com.nuvio.app.features.details.MetaTrailer(
                    id = media.trailer.id,
                    key = media.trailer.id,
                    name = "Official Trailer",
                    site = media.trailer.site ?: "YouTube",
                    type = "Trailer",
                    official = true,
                )
            )
        } else emptyList()

        val directors = media.staff.filter { it.role?.contains("Director", ignoreCase = true) == true }.mapNotNull { it.name }
        val writers = media.staff.filter {
            it.role?.contains("Composition", ignoreCase = true) == true ||
                it.role?.contains("Original", ignoreCase = true) == true ||
                it.role?.contains("Writer", ignoreCase = true) == true
        }.mapNotNull { it.name }

        val mappedVideos = if (isMovie) {
            emptyList()
        } else {
            (1..totalEpisodes).map { epNum ->
                val streamingEp = media.streamingEpisodes.getOrNull(epNum - 1)
                val cinemetaEp = cinemetaMeta?.videos?.firstOrNull { it.episode == epNum }
                val epData = episodeMap[epNum]

                val epTitle = streamingEp?.title?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.title?.takeIf { it.isNotBlank() }
                    ?: epData?.title?.takeIf { it.isNotBlank() }
                    ?: "Episode $epNum"

                val epOverview = cinemetaEp?.overview?.takeIf { it.isNotBlank() }
                    ?: epData?.overview

                val epThumbnail = streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: epData?.thumbnail
                    ?: cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: if (!armImdbId.isNullOrBlank()) "https://images.metahub.space/screenshot/medium/$armImdbId/$targetSeason/$epNum/img" else null

                val videoId = when {
                    !kitsuId.isNullOrBlank() -> "kitsu:$kitsuId:$epNum"
                    !armImdbId.isNullOrBlank() -> "$armImdbId:$targetSeason:$epNum"
                    else -> "anilist:$anilistId:$epNum"
                }

                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = targetSeason,
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
                description = cleanDescription,
                cast = cinemetaMeta.cast.ifEmpty { castPersons },
                productionCompanies = cinemetaMeta.productionCompanies.ifEmpty { animationStudios },
                networks = cinemetaMeta.networks.ifEmpty { networks },
                moreLikeThis = cinemetaMeta.moreLikeThis.ifEmpty { recommendations },
                trailers = cinemetaMeta.trailers.ifEmpty { trailers },
                director = cinemetaMeta.director.ifEmpty { directors },
                writer = cinemetaMeta.writer.ifEmpty { writers },
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
            description = cleanDescription,
            releaseInfo = if (media.episodes != null) "${media.episodes} Episodes" else null,
            status = media.status,
            imdbRating = formattedScore,
            genres = media.genres,
            country = "JP",
            language = "ja",
            hasScheduledVideos = media.status == "RELEASING",
            cast = castPersons,
            productionCompanies = animationStudios,
            networks = networks,
            moreLikeThis = recommendations,
            trailers = trailers,
            director = directors,
            writer = writers,
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

    data class ArmMapping(
        val imdbId: String?,
        val kitsuId: String?,
        val tmdbId: Int?,
        val season: Int,
    )

    private val armMappingCache = mutableMapOf<Int, ArmMapping>()
    private val kitsuIdCache = mutableMapOf<String, String>()

    suspend fun resolveArmMapping(anilistId: Int): ArmMapping {
        armMappingCache[anilistId]?.let { return it }
        return runCatching {
            val url = "https://arm.haglund.dev/api/v2/ids?source=anilist&id=$anilistId"
            val text = httpGetText(url) ?: return@runCatching ArmMapping(null, null, null, 1)
            val obj = json.parseToJsonElement(text).asJsonObjectOrNull() ?: return@runCatching ArmMapping(null, null, null, 1)
            val imdb = obj["imdb"].asStringOrNull()
            val kitsu = obj["kitsu"].asStringOrNull()
            val tmdb = obj["themoviedb"].asIntOrNull()
            val season = obj["thetvdb-season"].asIntOrNull()
                ?: obj["themoviedb-season"].asIntOrNull()
                ?: 1

            val mapping = ArmMapping(
                imdbId = imdb,
                kitsuId = kitsu,
                tmdbId = tmdb,
                season = if (season > 0) season else 1,
            )
            armMappingCache[anilistId] = mapping
            mapping
        }.getOrDefault(ArmMapping(null, null, null, 1))
    }

    suspend fun resolveArmImdbId(anilistId: Int): String? = resolveArmMapping(anilistId).imdbId

    suspend fun adaptCinemetaForAnilist(
        cinemetaMeta: MetaDetails,
        rawId: String,
    ): MetaDetails = withContext(Dispatchers.Default) {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId) ?: return@withContext cinemetaMeta
        val token = AnilistAuthRepository.token.value
        val media: AnilistMedia? = AnilistApi.getCachedMedia(anilistId) ?: AnilistApi.fetchMediaById(anilistId, token = token)
        val mapping = resolveArmMapping(anilistId)

        val anilistPoster = media?.coverImage?.extraLarge
            ?: media?.coverImage?.large
            ?: cinemetaMeta.poster

        val targetSeason = mapping.season
        val effectiveImdbId = mapping.imdbId ?: cinemetaMeta.id

        val isMovie = media?.format == "MOVIE" || (media?.episodes == 1 && media?.format != "TV")

        if (isMovie) {
            return@withContext cinemetaMeta.copy(
                id = rawId,
                type = "movie",
                name = media?.title?.displayTitle ?: cinemetaMeta.name,
                poster = anilistPoster,
                releaseInfo = media?.startDateYear?.toString() ?: cinemetaMeta.releaseInfo,
                defaultVideoId = effectiveImdbId,
            )
        }

        // For seasons, isolate and map that season's episodes cleanly
        val seasonVideos = if (targetSeason > 1 && cinemetaMeta.videos.any { it.season == targetSeason }) {
            cinemetaMeta.videos
                .filter { it.season == targetSeason }
                .mapIndexed { idx, v ->
                    val epNum = idx + 1
                    val streamingEp = media?.streamingEpisodes?.getOrNull(idx)
                    v.copy(
                        id = "$effectiveImdbId:$targetSeason:${v.episode ?: epNum}",
                        season = targetSeason,
                        episode = epNum,
                        title = streamingEp?.title?.takeIf { it.isNotBlank() } ?: v.title,
                        thumbnail = streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                            ?: v.thumbnail
                            ?: "https://images.metahub.space/screenshot/medium/$effectiveImdbId/$targetSeason/$epNum/img",
                    )
                }
        } else if (targetSeason > 1 && media?.episodes != null && media.episodes > 0) {
            (1..media.episodes).map { epNum ->
                val streamingEp = media.streamingEpisodes.getOrNull(epNum - 1)
                MetaVideo(
                    id = "$effectiveImdbId:$targetSeason:$epNum",
                    season = targetSeason,
                    episode = epNum,
                    title = streamingEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $epNum",
                    thumbnail = streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: "https://images.metahub.space/screenshot/medium/$effectiveImdbId/$targetSeason/$epNum/img",
                )
            }
        } else if (cinemetaMeta.videos.any { it.season == 1 }) {
            cinemetaMeta.videos
                .filter { it.season == 1 || it.season == null }
                .mapIndexed { idx, v ->
                    val epNum = idx + 1
                    val streamingEp = media?.streamingEpisodes?.getOrNull(idx)
                    v.copy(
                        id = "$effectiveImdbId:1:${v.episode ?: epNum}",
                        season = 1,
                        episode = epNum,
                        title = streamingEp?.title?.takeIf { it.isNotBlank() } ?: v.title,
                        thumbnail = streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                            ?: v.thumbnail
                            ?: "https://images.metahub.space/screenshot/medium/$effectiveImdbId/1/$epNum/img",
                    )
                }
        } else {
            cinemetaMeta.videos
        }

        val releaseYear = when {
            media?.startDateYear != null -> "${media.startDateYear}"
            else -> cinemetaMeta.releaseInfo
        }

        cinemetaMeta.copy(
            id = rawId,
            name = media?.title?.displayTitle ?: cinemetaMeta.name,
            poster = anilistPoster,
            releaseInfo = releaseYear,
            background = cinemetaMeta.background ?: "https://images.metahub.space/background/medium/$effectiveImdbId/img",
            logo = cinemetaMeta.logo ?: "https://images.metahub.space/logo/medium/$effectiveImdbId/img",
            videos = seasonVideos,
        )
    }

    private fun String.cleanAnilistDescription(): String {
        return this
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("(?i)\\(Source:.*?\\)"), "")
            .replace(Regex("(?i)\\[Written by.*?\\]"), "")
            .replace(Regex("(?i)Source:.*"), "")
            .trim()
    }

    private suspend fun resolveKitsuId(anilistId: Int, title: String): String? {
        val cacheKey = "$anilistId:$title"
        kitsuIdCache[cacheKey]?.let { return it }
        return runCatching {
            val armUrl = "https://arm.haglund.dev/api/v2/ids?source=anilist&id=$anilistId&include=kitsu"
            val armText = httpGetText(armUrl)
            if (!armText.isNullOrBlank()) {
                val obj = json.parseToJsonElement(armText).asJsonObjectOrNull()
                val kitsuId = obj?.get("kitsu").asStringOrNull()
                if (!kitsuId.isNullOrBlank()) {
                    kitsuIdCache[cacheKey] = kitsuId
                    return@runCatching kitsuId
                }
            }
            if (title.isNotBlank()) {
                val safeTitle = title.trim().replace(" ", "%20")
                val kitsuSearchUrl = "https://kitsu.io/api/edge/anime?filter%5Btext%5D=$safeTitle&page%5Blimit%5D=1"
                val searchRes = httpGetText(kitsuSearchUrl) ?: return@runCatching null
                val sObj = json.parseToJsonElement(searchRes).asJsonObjectOrNull() ?: return@runCatching null
                val id = sObj["data"].asJsonArrayOrNull()?.firstOrNull()?.asJsonObjectOrNull()?.get("id").asStringOrNull()
                if (!id.isNullOrBlank()) {
                    kitsuIdCache[cacheKey] = id
                    id
                } else null
            } else null
        }.getOrNull()
    }

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
