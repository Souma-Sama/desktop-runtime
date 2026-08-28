package com.nuvio.app.features.anilist.catalog

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistMedia
import com.nuvio.app.features.anilist.AnilistRelation
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.details.MetaVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

        // 1. Fetch base AniList media details
        val cached = AnilistApi.getCachedMedia(anilistId)
        val media: AnilistMedia = if (cached != null && cached.characters.isNotEmpty() && cached.recommendations.isNotEmpty()) {
            cached
        } else {
            AnilistApi.fetchMediaById(anilistId, token = token)
        } ?: return@withContext null

        // 2. Fast cache check for ARM mapping (0ms)
        val cachedArm = armMappingCache[anilistId] ?: runCatching {
            val diskCached = com.nuvio.app.features.fanart.FanartCacheStorage.get("arm:$anilistId")
            if (!diskCached.isNullOrBlank()) {
                val parts = diskCached.split("|")
                if (parts.size >= 5) {
                    ArmMapping(
                        imdbId = parts[0].takeIf(String::isNotBlank),
                        kitsuId = parts[1].takeIf(String::isNotBlank),
                        tmdbId = parts[2].toIntOrNull(),
                        tvdbId = parts[3].takeIf(String::isNotBlank),
                        season = parts[4].toIntOrNull() ?: 1,
                    ).also { armMappingCache[anilistId] = it }
                } else null
            } else null
        }.getOrNull()

        val armImdbId = cachedArm?.imdbId
        val targetSeason = cachedArm?.season ?: 1

        val backdrop = if (!armImdbId.isNullOrBlank()) {
            "https://images.metahub.space/background/medium/$armImdbId/img"
        } else {
            media.bannerImage
        }

        val logo = if (!armImdbId.isNullOrBlank()) {
            "https://images.metahub.space/logo/medium/$armImdbId/img"
        } else null

        val poster = media.coverImage?.extraLarge
            ?: media.coverImage?.large
            ?: media.coverImage?.medium

        val isMovie = media.format == "MOVIE"
        val contentType = if (isMovie) "movie" else "series"
        val totalEpisodes = media.episodes ?: media.streamingEpisodes.size.takeIf { it > 0 } ?: 12

        val cleanDescription = com.nuvio.app.core.format.cleanHtmlDescription(media.description)

        val castPersons = media.characters.mapNotNull { char ->
            val va = char.voiceActor
            val name = va?.name ?: char.name ?: return@mapNotNull null
            val characterRole = if (char.name != null && va?.name != null) "${char.name} (VA)" else char.name ?: "Cast"
            val photo = va?.image ?: char.image
            com.nuvio.app.features.details.MetaPerson(
                name = name,
                role = characterRole,
                photo = photo,
                tmdbId = va?.id ?: char.id,
            )
        }

        val animationStudios = media.studios.filter { it.isAnimationStudio }.mapNotNull { studio ->
            studio.name?.takeIf { it.isNotBlank() }?.let { name ->
                com.nuvio.app.features.details.MetaCompany(
                    name = name,
                )
            }
        }

        val networks = media.studios.filter { !it.isAnimationStudio }.mapNotNull { studio ->
            studio.name?.takeIf { it.isNotBlank() }?.let { name ->
                com.nuvio.app.features.details.MetaCompany(
                    name = name,
                )
            }
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
        val writers = media.staff.filter { it.role?.contains("Original Creator", ignoreCase = true) == true || it.role?.contains("Series Composition", ignoreCase = true) == true }.mapNotNull { it.name }

        val episodeOffset = resolveEpisodeOffset(
            media = media,
            targetSeason = targetSeason,
            cinemetaVideos = emptyList(),
        )

        // 3. Map Episodes directly from AniList streaming episodes with accurate Season/Part/Cour offset
        val mappedVideos = if (isMovie) {
            emptyList()
        } else if (targetSeason == 0) {
            val totalEps = media.episodes ?: media.streamingEpisodes.size.takeIf { it > 0 } ?: 1
            (1..totalEps).map { epNum ->
                val streamingEp = media.streamingEpisodes.getOrNull(epNum - 1)
                val epTitle = streamingEp?.title?.takeIf { it.isNotBlank() } ?: media.title?.displayTitle ?: "Special $epNum"
                val epThumbnail = if (!armImdbId.isNullOrBlank()) {
                    "https://episodes.metahub.space/$armImdbId/0/$epNum/w780.jpg"
                } else {
                    streamingEp?.thumbnail
                }
                val videoId = if (!armImdbId.isNullOrBlank()) {
                    "$armImdbId:0:$epNum"
                } else {
                    "anilist:$anilistId:$epNum"
                }
                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = 0,
                    episode = epNum,
                    overview = null,
                    thumbnail = epThumbnail,
                    released = null,
                    streams = emptyList(),
                )
            }
        } else {
            (1..totalEpisodes).map { epIdx ->
                val actualEpNumber = epIdx + episodeOffset
                val streamingEp = media.streamingEpisodes.getOrNull(epIdx - 1)
                val epTitle = streamingEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $actualEpNumber"
                val epThumbnail = if (!armImdbId.isNullOrBlank()) {
                    "https://episodes.metahub.space/$armImdbId/$targetSeason/$actualEpNumber/w780.jpg"
                } else {
                    streamingEp?.thumbnail
                }
                val videoId = if (!armImdbId.isNullOrBlank()) {
                    "$armImdbId:$targetSeason:$actualEpNumber"
                } else {
                    "anilist:$anilistId:$epIdx"
                }

                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = targetSeason,
                    episode = actualEpNumber,
                    overview = null,
                    thumbnail = epThumbnail,
                    released = null,
                    streams = emptyList(),
                )
            }
        }

        val formattedScore = if (media.averageScore != null && media.averageScore > 0) {
            val score = media.averageScore / 10.0
            "${(score * 10).toInt() / 10.0}"
        } else null

        val ratings = mutableListOf<com.nuvio.app.features.details.MetaExternalRating>()
        if (media.averageScore != null && media.averageScore > 0) {
            ratings.add(
                com.nuvio.app.features.details.MetaExternalRating(
                    source = "anilist",
                    value = media.averageScore.toDouble(),
                )
            )
        }

        val malScore = media.idMal?.let {
            runCatching {
                com.nuvio.app.features.anilist.AnilistApi.fetchMalScore(it)
            }.getOrNull()
        } ?: if (media.averageScore != null && media.averageScore > 0) {
            val approx = (media.averageScore / 10.0)
            kotlin.math.round(approx * 10.0) / 10.0
        } else null

        if (malScore != null && malScore > 0) {
            ratings.add(
                com.nuvio.app.features.details.MetaExternalRating(
                    source = "mal",
                    value = malScore,
                )
            )
        }

        val releaseYear = when {
            media.startDateYear != null -> "${media.startDateYear}"
            media.episodes != null -> "${media.episodes} Episodes"
            else -> null
        }

        MetaDetails(
            id = "ani_$anilistId",
            type = if (isMovie) "movie" else "series",
            name = media.title?.displayTitle.orEmpty(),
            poster = poster,
            background = backdrop,
            logo = logo,
            description = cleanDescription,
            releaseInfo = releaseYear,
            status = media.status,
            lastAirDate = media.endDateYear?.toString() ?: releaseYear,
            imdbRating = formattedScore,
            externalRatings = ratings,
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
            videos = mappedVideos,
            defaultVideoId = if (isMovie) (armImdbId ?: "anilist:$anilistId") else mappedVideos.firstOrNull()?.id,
        )
    }

    data class ArmMapping(
        val imdbId: String?,
        val kitsuId: String?,
        val tmdbId: Int?,
        val tvdbId: String?,
        val season: Int,
    )

    private val armMappingCache = mutableMapOf<Int, ArmMapping>()
    private val kitsuIdCache = mutableMapOf<String, String>()

    suspend fun resolveArmMapping(anilistId: Int): ArmMapping {
        armMappingCache[anilistId]?.let { return it }
        val diskCached = com.nuvio.app.features.fanart.FanartCacheStorage.get("arm:$anilistId")
        if (!diskCached.isNullOrBlank()) {
            val parts = diskCached.split("|")
            if (parts.size >= 5) {
                val mapping = ArmMapping(
                    imdbId = parts[0].takeIf(String::isNotBlank),
                    kitsuId = parts[1].takeIf(String::isNotBlank),
                    tmdbId = parts[2].toIntOrNull(),
                    tvdbId = parts[3].takeIf(String::isNotBlank),
                    season = parts[4].toIntOrNull() ?: 1,
                )
                armMappingCache[anilistId] = mapping
                return mapping
            }
        }

        return kotlinx.coroutines.withTimeoutOrNull(2500L) {
            runCatching {
                val url = "https://arm.haglund.dev/api/v2/ids?source=anilist&id=$anilistId"
                val text = httpGetText(url) ?: return@runCatching ArmMapping(null, null, null, null, 1)
                val obj = json.parseToJsonElement(text).asJsonObjectOrNull() ?: return@runCatching ArmMapping(null, null, null, null, 1)
                val imdb = obj["imdb"].asStringOrNull()
                val kitsu = obj["kitsu"].asStringOrNull()
                val tmdb = obj["themoviedb"].asIntOrNull()
                val tvdb = obj["thetvdb"].asStringOrNull() ?: obj["thetvdb"].asIntOrNull()?.toString()
                val season = obj["thetvdb-season"].asIntOrNull()
                    ?: obj["themoviedb-season"].asIntOrNull()
                    ?: 1

                val mapping = ArmMapping(
                    imdbId = imdb,
                    kitsuId = kitsu,
                    tmdbId = tmdb,
                    tvdbId = tvdb,
                    season = if (season >= 0) season else 1,
                )
                armMappingCache[anilistId] = mapping
                com.nuvio.app.features.fanart.FanartCacheStorage.put(
                    "arm:$anilistId",
                    "${mapping.imdbId.orEmpty()}|${mapping.kitsuId.orEmpty()}|${mapping.tmdbId ?: ""}|${mapping.tvdbId.orEmpty()}|${mapping.season}"
                )
                mapping
            }.getOrDefault(ArmMapping(null, null, null, null, 1))
        } ?: ArmMapping(null, null, null, null, 1)
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

        val isMovie = media?.format == "MOVIE"

        val recommendations = media?.recommendations?.mapNotNull { rec ->
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
        }.orEmpty()

        if (isMovie) {
            return@withContext cinemetaMeta.copy(
                id = rawId,
                type = "movie",
                name = media?.title?.displayTitle ?: cinemetaMeta.name,
                poster = anilistPoster,
                releaseInfo = media?.startDateYear?.toString() ?: cinemetaMeta.releaseInfo,
                defaultVideoId = effectiveImdbId,
                moreLikeThis = recommendations.ifEmpty { cinemetaMeta.moreLikeThis },
            )
        }

        val episodeOffset = resolveEpisodeOffset(
            media = media,
            targetSeason = targetSeason,
            cinemetaVideos = cinemetaMeta.videos,
        )

        // For seasons, isolate and map that season's episodes cleanly
        val seasonVideos = if (targetSeason == 0 && cinemetaMeta.videos.any { it.season == 0 }) {
            val specialVideos = cinemetaMeta.videos.filter { it.season == 0 }
            val matchedSpecial = findSpecialMatch(media, specialVideos)
            if (matchedSpecial != null) {
                listOf(
                    matchedSpecial.copy(
                        id = "$effectiveImdbId:0:${matchedSpecial.episode ?: 1}",
                        season = 0,
                        episode = matchedSpecial.episode ?: 1,
                        title = matchedSpecial.title?.takeIf { it.isNotBlank() } ?: media?.title?.displayTitle ?: "Special",
                        thumbnail = matchedSpecial.thumbnail?.takeIf { it.isNotBlank() }
                            ?: "https://episodes.metahub.space/$effectiveImdbId/0/${matchedSpecial.episode ?: 1}/w780.jpg",
                    )
                )
            } else {
                specialVideos.mapIndexed { idx, v ->
                    val epNum = v.episode ?: (idx + 1)
                    v.copy(
                        id = "$effectiveImdbId:0:$epNum",
                        season = 0,
                        episode = epNum,
                        title = v.title?.takeIf { it.isNotBlank() } ?: "Special $epNum",
                        thumbnail = v.thumbnail?.takeIf { it.isNotBlank() }
                            ?: "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg",
                    )
                }
            }
        } else if (targetSeason == 0) {
            val totalEps = media?.episodes ?: 1
            (1..totalEps).map { epNum ->
                MetaVideo(
                    id = "$effectiveImdbId:0:$epNum",
                    season = 0,
                    episode = epNum,
                    title = media?.title?.displayTitle ?: "Special $epNum",
                    thumbnail = "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg",
                )
            }
        } else if (targetSeason > 1 && cinemetaMeta.videos.any { it.season == targetSeason }) {
            val targetVideos = cinemetaMeta.videos.filter { it.season == targetSeason }
            val epCount = media?.episodes ?: targetVideos.size.takeIf { it > 0 } ?: 12
            (1..epCount).map { idx ->
                val actualEpNumber = idx + episodeOffset
                val cinemetaEp = cinemetaMeta.videos.firstOrNull { it.season == targetSeason && it.episode == actualEpNumber }
                    ?: cinemetaMeta.videos.firstOrNull { it.season == targetSeason && it.episode == null && cinemetaMeta.videos.indexOf(it) == actualEpNumber - 1 }
                val streamingEp = media?.streamingEpisodes?.getOrNull(idx - 1)
                MetaVideo(
                    id = "$effectiveImdbId:$targetSeason:$actualEpNumber",
                    season = targetSeason,
                    episode = actualEpNumber,
                    title = cinemetaEp?.title?.takeIf { it.isNotBlank() } ?: streamingEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $actualEpNumber",
                    thumbnail = cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg",
                    overview = cinemetaEp?.overview,
                    released = cinemetaEp?.released,
                    streams = cinemetaEp?.streams.orEmpty(),
                )
            }
        } else if (targetSeason > 1 && media?.episodes != null && media.episodes > 0) {
            (1..media.episodes).map { idx ->
                val actualEpNumber = idx + episodeOffset
                val streamingEp = media.streamingEpisodes.getOrNull(idx - 1)
                MetaVideo(
                    id = "$effectiveImdbId:$targetSeason:$actualEpNumber",
                    season = targetSeason,
                    episode = actualEpNumber,
                    title = streamingEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $actualEpNumber",
                    thumbnail = "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg",
                )
            }
        } else if (cinemetaMeta.videos.any { it.season == 1 }) {
            val targetVideos = cinemetaMeta.videos.filter { it.season == 1 || it.season == null }
            val epCount = media?.episodes ?: targetVideos.size.takeIf { it > 0 } ?: 12
            (1..epCount).map { idx ->
                val actualEpNumber = idx + episodeOffset
                val cinemetaEp = cinemetaMeta.videos.firstOrNull { (it.season == 1 || it.season == null) && it.episode == actualEpNumber }
                    ?: cinemetaMeta.videos.firstOrNull { (it.season == 1 || it.season == null) && it.episode == null && cinemetaMeta.videos.indexOf(it) == actualEpNumber - 1 }
                val streamingEp = media?.streamingEpisodes?.getOrNull(idx - 1)
                MetaVideo(
                    id = "$effectiveImdbId:1:$actualEpNumber",
                    season = 1,
                    episode = actualEpNumber,
                    title = cinemetaEp?.title?.takeIf { it.isNotBlank() } ?: streamingEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $actualEpNumber",
                    thumbnail = cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: "https://episodes.metahub.space/$effectiveImdbId/1/$actualEpNumber/w780.jpg",
                    overview = cinemetaEp?.overview,
                    released = cinemetaEp?.released,
                    streams = cinemetaEp?.streams.orEmpty(),
                )
            }
        } else {
            cinemetaMeta.videos
        }

        val releaseYear = when {
            media?.startDateYear != null -> "${media.startDateYear}"
            else -> cinemetaMeta.releaseInfo
        }

        val cleanDesc = com.nuvio.app.core.format.cleanHtmlDescription(media?.description)
            ?: cinemetaMeta.description

        cinemetaMeta.copy(
            id = rawId,
            name = media?.title?.displayTitle ?: cinemetaMeta.name,
            poster = anilistPoster,
            description = cleanDesc,
            releaseInfo = releaseYear,
            status = media?.status ?: cinemetaMeta.status,
            lastAirDate = media?.endDateYear?.toString() ?: releaseYear,
            background = cinemetaMeta.background ?: "https://images.metahub.space/background/medium/$effectiveImdbId/img",
            logo = cinemetaMeta.logo ?: "https://images.metahub.space/logo/medium/$effectiveImdbId/img",
            videos = seasonVideos,
            moreLikeThis = recommendations.ifEmpty { cinemetaMeta.moreLikeThis },
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

    private fun resolveEpisodeOffset(
        media: AnilistMedia?,
        targetSeason: Int,
        cinemetaVideos: List<MetaVideo>,
    ): Int {
        if (media == null) return 0
        val title = media.title?.displayTitle.orEmpty()
        val partNum = extractPartNumber(title)

        if (partNum > 1) {
            // 1. Recursively sum all prequels that belong to this split-cour sequence (handles different episode counts per cour!)
            val prequelSum = sumPrequelEpisodes(media.relations, title)
            if (prequelSum > 0) {
                return prequelSum
            }

            // 2. Fallback heuristic if prequel episode count was not specified in metadata
            val prequels = media.relations.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
            val prequelEps = prequels.mapNotNull { it.episodes }.firstOrNull { it > 0 }
                ?: media.episodes
                ?: (cinemetaVideos.count { it.season == targetSeason } / partNum).coerceAtLeast(11)

            return (partNum - 1) * prequelEps
        }

        return 0
    }

    private fun sumPrequelEpisodes(relations: List<AnilistRelation>, currentTitle: String): Int {
        var total = 0
        val prequels = relations.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
        for (prequel in prequels) {
            val pTitle = prequel.title?.displayTitle.orEmpty()
            val currentPart = extractPartNumber(currentTitle)
            val prequelPart = extractPartNumber(pTitle)
            if (currentPart > 1 && (prequelPart < currentPart || prequelPart == 1)) {
                val eps = prequel.episodes ?: 0
                total += eps + sumPrequelEpisodes(prequel.relations, pTitle.ifEmpty { currentTitle })
            }
        }
        return total
    }

    private fun extractPartNumber(title: String): Int {
        val m = Regex("(?i)(?:part|cour)\\s*(?:(\\d+)|(iv|iii|ii|i)\\b)").find(title)
        if (m != null) {
            val numStr = m.groupValues[1]
            if (numStr.isNotEmpty()) {
                return numStr.toIntOrNull() ?: 1
            }
            val roman = m.groupValues[2].lowercase()
            return when (roman) {
                "iv" -> 4
                "iii" -> 3
                "ii" -> 2
                "i" -> 1
                else -> 1
            }
        }
        val m2 = Regex("(?i)(\\d+)(?:nd|rd|th)\\s*cour").find(title)
        if (m2 != null) {
            return m2.groupValues[1].toIntOrNull() ?: 1
        }
        return 1
    }

    private fun findSpecialMatch(media: AnilistMedia?, specials: List<MetaVideo>): MetaVideo? {
        if (media == null || specials.isEmpty()) return specials.firstOrNull()
        if (specials.size == 1) return specials.first()

        val englishTitle = media.title?.english?.lowercase().orEmpty()
        val romajiTitle = media.title?.romaji?.lowercase().orEmpty()
        val displayTitle = media.title?.displayTitle?.lowercase().orEmpty()

        val stopWords = setOf("dr", "stone", "special", "episode", "ova", "ona", "the", "a", "an", "of", "no", "ni", "to", "part", "season")
        val keywords = (englishTitle + " " + romajiTitle + " " + displayTitle)
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()

        for (special in specials) {
            val sTitle = (special.title ?: "").lowercase()
            if (keywords.any { it in sTitle }) {
                return special
            }
        }

        return specials.firstOrNull()
    }
}
