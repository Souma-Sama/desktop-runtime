package com.nuvio.app.features.anilist.catalog

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.encodeUnsafeHttpUrlCharacters
import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpGetTextWithHeaders
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

    suspend fun resolveMetaDetails(rawId: String): MetaDetails? = coroutineScope {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId) ?: return@coroutineScope null
        val token = AnilistAuthRepository.token.value

        // 1. Fast cache check for ARM mapping (0ms)
        val cachedArm = armMappingCache[anilistId]

        // Fetch ARM mapping concurrently with AniList media fetch!
        val armDeferred = async {
            cachedArm ?: runCatching {
                kotlinx.coroutines.withTimeoutOrNull(2000L) { resolveArmMapping(anilistId) }
            }.getOrNull() ?: ArmMapping(null, null, null, null, 1)
        }

        // Fetch base AniList media details
        val cached = AnilistApi.getCachedMedia(anilistId)
        val media: AnilistMedia = if (cached != null && cached.characters.isNotEmpty() && cached.recommendations.isNotEmpty() && cached.studios.isNotEmpty()) {
            cached
        } else {
            AnilistApi.fetchMediaById(anilistId, token = token) ?: cached
        } ?: return@coroutineScope null

        val armMapping = armDeferred.await()

        val effectiveImdbId = armMapping.imdbId
            ?: media.relations.firstOrNull { it.relationType in listOf("PARENT", "PREQUEL", "SOURCE", "MAIN", "ALTERNATIVE") }?.let {
                armMappingCache[it.id]?.imdbId ?: runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(1200L) { resolveArmMapping(it.id) }
                }.getOrNull()?.imdbId
            }

        val isSpecial = isSpecialAnime(media)
        val targetSeason = when {
            armMapping.season == 0 -> 0
            isSpecial -> 0
            else -> armMapping.season
        }

        val kitsuId = armMapping.kitsuId?.removePrefix("kitsu:")?.takeIf { it.isNotBlank() }
            ?: resolveKitsuId(anilistId, media.title?.displayTitle.orEmpty())

        val kitsuDeferred = async {
            if (!kitsuId.isNullOrBlank()) {
                runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(2000L) { fetchKitsuEpisodes(kitsuId) }
                }.getOrNull() ?: emptyMap()
            } else emptyMap()
        }

        val tmdbEpisodesDeferred = async {
            val tmdbSettings = com.nuvio.app.features.tmdb.TmdbSettingsRepository.snapshot()
            if (tmdbSettings.hasApiKey) {
                val effectiveTmdbId = armMapping.tmdbId?.toString()
                    ?: if (!effectiveImdbId.isNullOrBlank()) com.nuvio.app.features.tmdb.TmdbService.ensureTmdbId(effectiveImdbId, "tv") else null
                if (!effectiveTmdbId.isNullOrBlank()) {
                    runCatching {
                        kotlinx.coroutines.withTimeoutOrNull(2000L) {
                            val seasonsToFetch = listOf(targetSeason, 1, 0).distinct()
                            com.nuvio.app.features.tmdb.TmdbMetadataService.fetchEpisodeEnrichment(
                                tmdbId = effectiveTmdbId,
                                seasonNumbers = seasonsToFetch,
                                language = tmdbSettings.language,
                            )
                        }
                    }.getOrNull() ?: emptyMap()
                } else emptyMap()
            } else emptyMap()
        }

        val malDeferred = async {
            val idMal = media.idMal
            if (idMal != null) {
                runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(1500L) {
                        com.nuvio.app.features.anilist.AnilistApi.fetchMalMetadata(idMal)
                    }
                }.getOrNull()
            } else null
        }

        val ytTrailersDeferred = async {
            fetchYoutubeAnimeTrailers(media.title?.displayTitle.orEmpty())
        }

        val kitsuEpisodes = kitsuDeferred.await()
        val tmdbEpisodes = tmdbEpisodesDeferred.await()

        val poster = media.coverImage?.extraLarge
            ?: media.coverImage?.large
            ?: media.coverImage?.medium

        val episode1Thumb = kitsuEpisodes[1]?.thumbnail?.takeIf { it.isNotBlank() }
            ?: media.streamingEpisodes.firstOrNull()?.thumbnail?.takeIf { it.isNotBlank() }

        val backdrop = if (!effectiveImdbId.isNullOrBlank()) {
            "https://images.metahub.space/background/medium/$effectiveImdbId/img"
        } else {
            media.bannerImage ?: episode1Thumb ?: poster
        }

        val logo = if (!effectiveImdbId.isNullOrBlank()) {
            "https://images.metahub.space/logo/medium/$effectiveImdbId/img"
        } else null

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
                    logo = AnimeStudioLogos.findLogo(name),
                )
            }
        }

        val networks = media.studios.filter { !it.isAnimationStudio }.mapNotNull { studio ->
            studio.name?.takeIf { it.isNotBlank() }?.let { name ->
                com.nuvio.app.features.details.MetaCompany(
                    name = name,
                    logo = AnimeStudioLogos.findLogo(name),
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

        val primaryTrailer = if (media.trailer != null && media.trailer.id != null) {
            com.nuvio.app.features.details.MetaTrailer(
                id = media.trailer.id,
                key = media.trailer.id,
                name = "Official Trailer",
                site = media.trailer.site ?: "YouTube",
                type = "Trailer",
                official = true,
            )
        } else null

        val ytTrailers = ytTrailersDeferred.await()

        val seenTrailerKeys = mutableSetOf<String>()
        val trailers = mutableListOf<com.nuvio.app.features.details.MetaTrailer>()

        if (primaryTrailer != null && seenTrailerKeys.add(primaryTrailer.key)) {
            trailers.add(primaryTrailer)
        }
        for (t in ytTrailers) {
            if (seenTrailerKeys.add(t.key)) {
                trailers.add(t)
            }
        }

        val directors = media.staff.filter { it.role?.contains("Director", ignoreCase = true) == true }.mapNotNull { it.name }
        val writers = media.staff.filter { it.role?.contains("Original Creator", ignoreCase = true) == true || it.role?.contains("Series Composition", ignoreCase = true) == true }.mapNotNull { it.name }

        val episodeOffset = resolveEpisodeOffset(
            media = media,
            targetSeason = targetSeason,
            cinemetaVideos = emptyList(),
        )

        val mappedVideos = if (isMovie) {
            emptyList()
        } else if (targetSeason == 0) {
            val totalEps = media.episodes ?: media.streamingEpisodes.size.takeIf { it > 0 } ?: 1
            val startsAtZero = media.description?.contains("Includes Episode 0", ignoreCase = true) == true ||
                (media.description?.contains("Episode 0", ignoreCase = true) == true && media.streamingEpisodes.any { it.title?.contains("Episode 0", ignoreCase = true) == true })
            val epRange = if (startsAtZero) (0 until totalEps) else (1..totalEps)

            epRange.map { epNum ->
                val streamingEp = if (startsAtZero) media.streamingEpisodes.getOrNull(epNum) else media.streamingEpisodes.getOrNull(epNum - 1)
                val kitsuEp = kitsuEpisodes[epNum] ?: (if (startsAtZero && epNum == 0) kitsuEpisodes[0] else null)
                val tmdbEp = tmdbEpisodes[0 to epNum] ?: tmdbEpisodes[targetSeason to epNum]
                val rawTitle = tmdbEp?.title?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                    ?: if (epNum == 0) "Episode 0" else media.title?.displayTitle
                val epTitle = cleanEpisodeTitle(rawTitle, epNum)
                val metahubEpThumb = if (!effectiveImdbId.isNullOrBlank()) {
                    "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg"
                } else null
                val epThumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: metahubEpThumb
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$epNum" else "anilist:$anilistId:$epNum"
                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = 0,
                    episode = epNum,
                    overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: kitsuEp?.overview,
                    thumbnail = epThumbnail,
                    runtime = tmdbEp?.runtimeMinutes ?: media.duration,
                    released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate,
                    streams = emptyList(),
                )
            }
        } else {
            (1..totalEpisodes).map { epIdx ->
                val actualEpNumber = epIdx + episodeOffset
                val streamingEp = media.streamingEpisodes.getOrNull(epIdx - 1)
                val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[epIdx]
                val tmdbEp = tmdbEpisodes[targetSeason to actualEpNumber]
                    ?: tmdbEpisodes[1 to actualEpNumber]
                    ?: tmdbEpisodes[targetSeason to epIdx]
                    ?: tmdbEpisodes[1 to epIdx]
                val rawTitle = tmdbEp?.title?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val metahubEpThumb = if (!effectiveImdbId.isNullOrBlank()) {
                    "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg"
                } else null
                val epThumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: metahubEpThumb
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$actualEpNumber" else "anilist:$anilistId:$epIdx"

                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = targetSeason,
                    episode = actualEpNumber,
                    overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: kitsuEp?.overview,
                    thumbnail = epThumbnail,
                    runtime = tmdbEp?.runtimeMinutes ?: media.duration,
                    released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate,
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

        val malMeta = malDeferred.await()

        val malScore = malMeta?.score ?: if (media.averageScore != null && media.averageScore > 0) {
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

        val ageRating = malMeta?.ageRating ?: if (media.genres.contains("Hentai")) "18+" else "TV-14"
        val runtime = if (media.duration != null && media.duration > 0) "${media.duration} min" else null

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
            ageRating = ageRating,
            runtime = runtime,
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
            defaultVideoId = if (isMovie) (if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId" else "anilist:$anilistId") else mappedVideos.firstOrNull()?.id,
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

        val isSpecial = isSpecialAnime(media)
        val targetSeason = when {
            mapping.season == 0 -> 0
            isSpecial -> 0
            else -> mapping.season
        }
        val effectiveImdbId = mapping.imdbId
            ?: cinemetaMeta.id.takeIf { it.startsWith("tt") }
            ?: media?.relations?.firstOrNull { it.relationType in listOf("PARENT", "PREQUEL", "SOURCE", "MAIN", "ALTERNATIVE") }?.let {
                resolveArmMapping(it.id).imdbId
            }

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

        val kitsuId = mapping.kitsuId?.removePrefix("kitsu:")?.takeIf { it.isNotBlank() }
            ?: resolveKitsuId(anilistId, media?.title?.displayTitle.orEmpty())

        val kitsuEpisodes = if (!kitsuId.isNullOrBlank()) {
            fetchKitsuEpisodes(kitsuId)
        } else emptyMap()

        val tmdbSettings = com.nuvio.app.features.tmdb.TmdbSettingsRepository.snapshot()
        val tmdbEpisodes = if (tmdbSettings.hasApiKey) {
            val effectiveTmdbId = mapping.tmdbId?.toString()
                ?: if (!effectiveImdbId.isNullOrBlank()) com.nuvio.app.features.tmdb.TmdbService.ensureTmdbId(effectiveImdbId, "tv") else null
            if (!effectiveTmdbId.isNullOrBlank()) {
                val seasonsToFetch = listOf(targetSeason, 1, 0).distinct()
                com.nuvio.app.features.tmdb.TmdbMetadataService.fetchEpisodeEnrichment(
                    tmdbId = effectiveTmdbId,
                    seasonNumbers = seasonsToFetch,
                    language = tmdbSettings.language,
                )
            } else emptyMap()
        } else emptyMap()

        if (isMovie) {
            return@withContext cinemetaMeta.copy(
                id = rawId,
                type = "movie",
                name = media?.title?.displayTitle ?: cinemetaMeta.name,
                poster = anilistPoster,
                releaseInfo = media?.startDateYear?.toString() ?: cinemetaMeta.releaseInfo,
                defaultVideoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId" else "anilist:$anilistId",
                moreLikeThis = recommendations.ifEmpty { cinemetaMeta.moreLikeThis },
            )
        }

        val episodeOffset = resolveEpisodeOffset(
            media = media,
            targetSeason = targetSeason,
            cinemetaVideos = cinemetaMeta.videos,
        )

        // For seasons, isolate and map that season's episodes cleanly using Kitsu IDs
        val seasonVideos = if (targetSeason == 0 && cinemetaMeta.videos.any { it.season == 0 }) {
            val specialVideos = cinemetaMeta.videos.filter { it.season == 0 }
            val matchedSpecial = findSpecialMatch(media, specialVideos)
            if (matchedSpecial != null) {
                val epNum = matchedSpecial.episode ?: 1
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$epNum" else "anilist:$anilistId:$epNum"
                val kitsuEp = kitsuEpisodes[epNum] ?: (if (epNum == 0) kitsuEpisodes[0] else null)
                val tmdbEp = tmdbEpisodes[0 to epNum] ?: tmdbEpisodes[targetSeason to epNum]
                listOf(
                    matchedSpecial.copy(
                        id = videoId,
                        season = 0,
                        episode = epNum,
                        title = tmdbEp?.title?.takeIf { it.isNotBlank() } ?: matchedSpecial.title?.takeIf { it.isNotBlank() } ?: media?.title?.displayTitle ?: "Special",
                        thumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() } ?: matchedSpecial.thumbnail?.takeIf { it.isNotBlank() }
                            ?: "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg",
                        overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: matchedSpecial.overview ?: kitsuEp?.overview,
                        released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate ?: matchedSpecial.released,
                    )
                )
            } else {
                specialVideos.mapIndexed { idx, v ->
                    val epNum = v.episode ?: (idx + 1)
                    val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$epNum" else "anilist:$anilistId:$epNum"
                    val kitsuEp = kitsuEpisodes[epNum] ?: (if (epNum == 0) kitsuEpisodes[0] else null)
                    val tmdbEp = tmdbEpisodes[0 to epNum] ?: tmdbEpisodes[targetSeason to epNum]
                    v.copy(
                        id = videoId,
                        season = 0,
                        episode = epNum,
                        title = tmdbEp?.title?.takeIf { it.isNotBlank() } ?: v.title?.takeIf { it.isNotBlank() } ?: "Special $epNum",
                        thumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() } ?: v.thumbnail?.takeIf { it.isNotBlank() }
                            ?: "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg",
                        overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: v.overview ?: kitsuEp?.overview,
                        released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate ?: v.released,
                    )
                }
            }
        } else if (targetSeason == 0) {
            val totalEps = media?.episodes ?: 1
            val startsAtZero = media?.description?.contains("Includes Episode 0", ignoreCase = true) == true ||
                (media?.description?.contains("Episode 0", ignoreCase = true) == true && media.streamingEpisodes.any { it.title?.contains("Episode 0", ignoreCase = true) == true })
            val epRange = if (startsAtZero) (0 until totalEps) else (1..totalEps)

            epRange.map { epNum ->
                val streamingEp = if (startsAtZero) media?.streamingEpisodes?.getOrNull(epNum) else media?.streamingEpisodes?.getOrNull(epNum - 1)
                val kitsuEp = kitsuEpisodes[epNum] ?: (if (startsAtZero && epNum == 0) kitsuEpisodes[0] else null)
                val tmdbEp = tmdbEpisodes[0 to epNum] ?: tmdbEpisodes[targetSeason to epNum]
                val rawTitle = tmdbEp?.title?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                    ?: if (epNum == 0) "Episode 0" else media?.title?.displayTitle
                val epTitle = cleanEpisodeTitle(rawTitle, epNum)
                val epThumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail
                    ?: "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg"
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$epNum" else "anilist:$anilistId:$epNum"
                MetaVideo(
                    id = videoId,
                    season = 0,
                    episode = epNum,
                    title = epTitle,
                    thumbnail = epThumbnail,
                    overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: kitsuEp?.overview,
                    runtime = tmdbEp?.runtimeMinutes ?: media?.duration,
                    released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate,
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
                val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx]
                val tmdbEp = tmdbEpisodes[targetSeason to actualEpNumber]
                    ?: tmdbEpisodes[1 to actualEpNumber]
                    ?: tmdbEpisodes[targetSeason to idx]
                    ?: tmdbEpisodes[1 to idx]
                val rawTitle = tmdbEp?.title?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val epThumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail
                    ?: "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg"
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$actualEpNumber" else "anilist:$anilistId:$actualEpNumber"
                MetaVideo(
                    id = videoId,
                    season = targetSeason,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = epThumbnail,
                    overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: kitsuEp?.overview ?: cinemetaEp?.overview,
                    released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate ?: cinemetaEp?.released,
                    streams = cinemetaEp?.streams.orEmpty(),
                    runtime = tmdbEp?.runtimeMinutes ?: media?.duration ?: cinemetaEp?.runtime,
                )
            }
        } else if (targetSeason > 1 && media?.episodes != null && media.episodes > 0) {
            (1..media.episodes).map { idx ->
                val actualEpNumber = idx + episodeOffset
                val streamingEp = media.streamingEpisodes.getOrNull(idx - 1)
                val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx]
                val tmdbEp = tmdbEpisodes[targetSeason to actualEpNumber]
                    ?: tmdbEpisodes[1 to actualEpNumber]
                    ?: tmdbEpisodes[targetSeason to idx]
                    ?: tmdbEpisodes[1 to idx]
                val rawTitle = tmdbEp?.title?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val epThumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail
                    ?: "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg"
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$actualEpNumber" else "anilist:$anilistId:$actualEpNumber"
                MetaVideo(
                    id = videoId,
                    season = targetSeason,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = epThumbnail,
                    overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: kitsuEp?.overview,
                    runtime = tmdbEp?.runtimeMinutes ?: media.duration,
                    released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate,
                )
            }
        } else {
            val targetVideos = cinemetaMeta.videos.filter { it.season == 1 || it.season == null }
            val epCount = media?.episodes ?: targetVideos.size.takeIf { it > 0 } ?: 12
            (1..epCount).map { idx ->
                val actualEpNumber = idx + episodeOffset
                val cinemetaEp = cinemetaMeta.videos.firstOrNull { (it.season == 1 || it.season == null) && it.episode == actualEpNumber }
                    ?: cinemetaMeta.videos.firstOrNull { (it.season == 1 || it.season == null) && it.episode == null && cinemetaMeta.videos.indexOf(it) == actualEpNumber - 1 }
                val streamingEp = media?.streamingEpisodes?.getOrNull(idx - 1)
                val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx]
                val tmdbEp = tmdbEpisodes[1 to actualEpNumber]
                    ?: tmdbEpisodes[targetSeason to actualEpNumber]
                    ?: tmdbEpisodes[1 to idx]
                    ?: tmdbEpisodes[targetSeason to idx]
                val rawTitle = tmdbEp?.title?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val epThumbnail = tmdbEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail
                    ?: "https://episodes.metahub.space/$effectiveImdbId/1/$actualEpNumber/w780.jpg"
                val videoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId:$actualEpNumber" else "anilist:$anilistId:$actualEpNumber"
                MetaVideo(
                    id = videoId,
                    season = 1,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = epThumbnail,
                    overview = tmdbEp?.overview?.takeIf { it.isNotBlank() } ?: kitsuEp?.overview ?: cinemetaEp?.overview,
                    released = tmdbEp?.airDate?.takeIf { it.isNotBlank() } ?: kitsuEp?.airdate ?: cinemetaEp?.released,
                    streams = cinemetaEp?.streams.orEmpty(),
                    runtime = tmdbEp?.runtimeMinutes ?: media?.duration ?: cinemetaEp?.runtime,
                )
            }
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

    private fun cleanEpisodeTitle(rawTitle: String?, episodeNum: Int): String {
        if (rawTitle.isNullOrBlank()) return "Episode $episodeNum"
        val trimmed = rawTitle.trim()
        val cleaned = trimmed
            .replace(Regex("^(?:Episode|Ep\\.?|E|Special|#)\\s*\\d+\\s*[-:–—|~]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:Episode|Ep\\.?|E|Special|#)\\s*\\d+\\s*$", RegexOption.IGNORE_CASE), "")
            .trim()
        return cleaned.ifEmpty { "Episode $episodeNum" }
    }

    private data class KitsuEpisodeData(
        val title: String?,
        val overview: String?,
        val thumbnail: String?,
        val airdate: String? = null,
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
            val airdate = attrs["airdate"].asStringOrNull() ?: attrs["released"].asStringOrNull()

            result[epNum] = KitsuEpisodeData(
                title = epTitle,
                overview = overview,
                thumbnail = thumbnail,
                airdate = airdate,
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

    private fun isSpecialAnime(media: AnilistMedia?): Boolean {
        if (media == null) return false
        val format = media.format?.uppercase()
        if (format == "SPECIAL" || format == "OVA") return true
        val fullText = (
            media.title?.displayTitle.orEmpty() + " " +
            media.title?.romaji.orEmpty() + " " +
            media.title?.english.orEmpty() + " " +
            media.description.orEmpty()
        ).lowercase()

        return fullText.contains("mahou") ||
            fullText.contains("special") ||
            fullText.contains("mini anime") ||
            fullText.contains("chibi") ||
            fullText.contains("parody") ||
            fullText.contains("picture drama") ||
            fullText.contains("omake") ||
            fullText.contains("bonus") ||
            fullText.contains("includes episode 0") ||
            (format == "ONA" && (fullText.contains("short") || fullText.contains("sp") || (media.episodes != null && media.episodes <= 13 && media.duration != null && media.duration <= 10)))
    }

    private val trailerCache = mutableMapOf<String, List<com.nuvio.app.features.details.MetaTrailer>>()

    private suspend fun fetchYoutubeAnimeTrailers(animeTitle: String): List<com.nuvio.app.features.details.MetaTrailer> {
        val cleanTitle = animeTitle.trim()
        if (cleanTitle.isBlank()) return emptyList()

        trailerCache[cleanTitle]?.let { return it }

        return runCatching {
            withTimeoutOrNull(2000L) {
                val encodedQuery = cleanTitle.encodeUnsafeHttpUrlCharacters()
                val url = "https://www.youtube.com/results?search_query=$encodedQuery+anime+official+trailer"
                val html = httpGetTextWithHeaders(
                    url = url,
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                        "Accept-Language" to "en-US,en;q=0.9",
                    ),
                )

                val regex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"(.*?)"\}""")
                val matches = regex.findAll(html)

                val seen = mutableSetOf<String>()
                val trailers = mutableListOf<com.nuvio.app.features.details.MetaTrailer>()
                val keywords = listOf("trailer", "pv", "teaser", "preview", "official", "promo", "sub", "dub")

                for (match in matches) {
                    val vid = match.groupValues.getOrNull(1) ?: continue
                    val rawName = match.groupValues.getOrNull(2) ?: continue
                    if (!seen.add(vid)) continue

                    val lower = rawName.lowercase()
                    if (keywords.any { lower.contains(it) }) {
                        val trailerType = when {
                            lower.contains("teaser") -> "Teaser"
                            lower.contains("pv") || lower.contains("preview") || lower.contains("promo") -> "Promo Video"
                            lower.contains("clip") -> "Clip"
                            else -> "Trailer"
                        }

                        val cleanName = rawName
                            .replace("&quot;", "\"")
                            .replace("&amp;", "&")
                            .replace("&#39;", "'")
                            .replace(Regex("""\s*\|\s*Crunchyroll""", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("""\s*\|\s*Netflix""", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("""\s*\|\s*TOHO animation""", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("""\s*\|\s*Aniplex""", RegexOption.IGNORE_CASE), "")
                            .trim()

                        trailers.add(
                            com.nuvio.app.features.details.MetaTrailer(
                                id = vid,
                                key = vid,
                                name = cleanName.ifBlank { "Official Trailer" },
                                site = "YouTube",
                                type = trailerType,
                                official = true,
                            ),
                        )

                        if (trailers.size >= 8) break
                    }
                }
                trailerCache[cleanTitle] = trailers
                trailers
            }
        }.getOrNull().orEmpty()
    }
}
