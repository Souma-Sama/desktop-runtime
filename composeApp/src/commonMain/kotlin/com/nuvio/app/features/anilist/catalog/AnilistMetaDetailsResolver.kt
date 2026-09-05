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
import com.nuvio.app.features.anilist.streams.AnimeStreamIdManager
import com.nuvio.app.features.artwork.MetaHubArtwork
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.details.MetaVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

    private val resolvedMetaDetailsCache = mutableMapOf<String, MetaDetails>()
    private val episodeOffsetCache = mutableMapOf<Int, Int>()

    fun buildBaseMetaFromAnilistMedia(
        media: AnilistMedia,
        backdrop: String? = null,
        logo: String? = null,
        season: Int = 1,
        imdbId: String? = null,
        requestedId: String? = null,
    ): MetaDetails {
        val anilistId = media.id
        val isMovie = media.format == "MOVIE"
        val totalEpisodes = media.episodes ?: media.streamingEpisodes.size.takeIf { it > 0 } ?: 12

        val poster = media.coverImage?.bestUrl
            ?: media.coverImage?.extraLarge
            ?: media.coverImage?.large
            ?: media.coverImage?.medium

        val effectiveLogo = logo ?: MetaHubArtwork.getLogoUrl("ani_$anilistId")
        val cleanDescription = com.nuvio.app.core.format.cleanHtmlDescription(media.description)
        val castPersons = buildCategorizedCast(media)

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
            val recPoster = rec.coverImage?.bestUrl
                ?: rec.coverImage?.extraLarge
                ?: rec.coverImage?.large
                ?: rec.coverImage?.medium
                ?: return@mapNotNull null
            val recScore = if (rec.averageScore != null && rec.averageScore > 0) rec.averageScore / 10.0 else null
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$recId",
                type = recType,
                name = rec.title?.displayTitle.orEmpty(),
                poster = recPoster,
                banner = rec.bannerImage,
                logo = MetaHubArtwork.getLogoUrl("ani_$recId"),
                description = null,
                releaseInfo = if (rec.episodes != null) "${rec.episodes} Ep" else null,
                imdbRating = recScore?.let { "${((it * 10).toInt()) / 10.0}" },
                anilistScore = if (rec.averageScore != null && rec.averageScore > 0) rec.averageScore.toDouble() else null,
            )
        }

        val relations = media.relations.mapNotNull { rel ->
            val relTitle = rel.title?.displayTitle?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val relPoster = rel.coverImage?.bestUrl
                ?: rel.coverImage?.extraLarge
                ?: rel.coverImage?.large
                ?: rel.coverImage?.medium
            val isRelMovie = rel.format == "MOVIE" || rel.episodes == 1
            val relType = if (isRelMovie) "movie" else "series"
            val relationLabel = when (rel.relationType?.uppercase()) {
                "PREQUEL" -> "Prequel"
                "SEQUEL" -> "Sequel"
                "PARENT" -> "Parent Story"
                "SIDE_STORY" -> "Side Story"
                "SPIN_OFF" -> "Spin-Off"
                "ALTERNATIVE" -> "Alternative"
                "SUMMARY" -> "Summary"
                "CHARACTER" -> "Character"
                "OTHER" -> "Other"
                else -> rel.relationType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Related"
            }
            com.nuvio.app.features.details.MetaRelation(
                id = "ani_${rel.id}",
                type = relType,
                relationType = relationLabel,
                title = relTitle,
                poster = relPoster,
                format = rel.format,
                episodes = rel.episodes,
                status = rel.status,
                averageScore = rel.averageScore,
            )
        }

        val nextAiringCountdown = media.nextAiringEpisode?.let { nextEp ->
            val epNum = nextEp.episode ?: return@let null
            val timeUntil = nextEp.timeUntilAiring
            if (timeUntil != null && timeUntil > 0) {
                val days = timeUntil / 86400
                val hours = (timeUntil % 86400) / 3600
                val mins = (timeUntil % 3600) / 60
                val timeFormatted = when {
                    days > 0 -> "${days}d ${hours}h"
                    hours > 0 -> "${hours}h ${mins}m"
                    else -> "${mins}m"
                }
                "Ep $epNum in $timeFormatted"
            } else {
                "Ep $epNum Airing Soon"
            }
        }

        val primaryTrailer = if (media.trailer != null && media.trailer.id != null) {
            listOf(
                MetaTrailer(
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

        val mappedVideos = if (isMovie) {
            emptyList()
        } else {
            val startsAtZero = media.description?.contains("Includes Episode 0", ignoreCase = true) == true ||
                (media.description?.contains("Episode 0", ignoreCase = true) == true && media.streamingEpisodes.any { it.title?.contains("Episode 0", ignoreCase = true) == true })
            val epRange = if (startsAtZero) (0 until totalEpisodes) else (1..totalEpisodes)

            epRange.map { idx ->
                val epNum = idx
                val streamingEp = if (startsAtZero) media.streamingEpisodes.getOrNull(idx) else media.streamingEpisodes.getOrNull(idx - 1)
                val rawTitle = streamingEp?.title?.takeIf { it.isNotBlank() } ?: "Episode $epNum"
                val epTitle = cleanEpisodeTitle(rawTitle, epNum)
                val streamingThumb = streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                val metahubThumb = if (!imdbId.isNullOrBlank()) {
                    "https://episodes.metahub.space/$imdbId/$season/$epNum/w780.jpg"
                } else null
                val thumb = metahubThumb ?: streamingThumb
                val videoId = resolveEpisodeVideoId(
                    anilistId = anilistId,
                    season = season,
                    episode = epNum,
                    effectiveImdbId = imdbId,
                    kitsuId = null,
                )
                MetaVideo(
                    id = videoId,
                    title = epTitle,
                    season = season,
                    episode = epNum,
                    overview = null,
                    thumbnail = thumb,
                    fallbackThumbnail = streamingThumb,
                    runtime = media.duration,
                    released = resolveEpisodeAirDate(epNum, null, null, media),
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

        val releaseYear = when {
            media.startDateYear != null -> "${media.startDateYear}"
            media.episodes != null -> "${media.episodes} Episodes"
            else -> null
        }

        val ageRating = if (media.genres.contains("Hentai")) "18+" else "TV-14"
        val runtime = if (media.duration != null && media.duration > 0) "${media.duration} min" else null
        val effectiveId = requestedId ?: "ani_$anilistId"

        return MetaDetails(
            id = effectiveId,
            type = if (isMovie) "movie" else "series",
            name = media.title?.displayTitle.orEmpty(),
            poster = poster,
            background = backdrop,
            logo = effectiveLogo,
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
            relations = relations,
            nextAiringEpisode = nextAiringCountdown,
            trailers = primaryTrailer,
            director = directors,
            writer = writers,
            videos = mappedVideos,
            defaultVideoId = if (isMovie) "anilist:$anilistId" else mappedVideos.firstOrNull()?.id,
        )
    }

    suspend fun resolveMetaDetails(rawId: String): MetaDetails? = coroutineScope {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId) ?: return@coroutineScope null
        val token = AnilistAuthRepository.token.value
        val cached = AnilistApi.getCachedMedia(anilistId)
            ?: AnilistTrackerCoordinator.getCachedMedia(anilistId)
            ?: AnilistTrackerCoordinator.getCachedMedia(rawId)

        // If we already have cached media (from home/catalog/search/anichart), construct baseMeta instantly!
        if (cached != null) {
            val cachedArm = armMappingCache[anilistId]
            val isSpecial = isSpecialAnime(cached)
            val targetSeason = when {
                cachedArm?.season == 0 -> 0
                isSpecial -> 0
                else -> cachedArm?.season ?: 1
            }
            val effectiveImdbId = cachedArm?.imdbId ?: MetaHubArtwork.extractImdbId(rawId)
            val backdrop = if (!effectiveImdbId.isNullOrBlank()) {
                "https://images.metahub.space/background/medium/$effectiveImdbId/img"
            } else cached.bannerImage

            val logo = if (!isSpecial && targetSeason != 0 && !effectiveImdbId.isNullOrBlank()) {
                "https://images.metahub.space/logo/medium/$effectiveImdbId/img"
            } else null

            AnimeStreamIdManager.registerOptions(
                anilistId = anilistId,
                imdbId = effectiveImdbId,
                kitsuId = cachedArm?.kitsuId,
                tmdbId = cachedArm?.tmdbId,
                season = targetSeason,
            )

            return@coroutineScope buildBaseMetaFromAnilistMedia(
                media = cached,
                backdrop = backdrop,
                logo = logo,
                season = targetSeason,
                imdbId = effectiveImdbId,
                requestedId = rawId,
            )
        }

        val mediaDeferred = async {
            runCatching {
                withTimeoutOrNull(8000L) {
                    AnilistApi.fetchMediaById(anilistId, token = token)
                }
            }.getOrNull() ?: cached
        }

        val armDeferred = async {
            val cachedArm = armMappingCache[anilistId]
            cachedArm ?: runCatching {
                withTimeoutOrNull(3000L) { resolveArmMapping(anilistId) }
            }.getOrNull() ?: ArmMapping(null, null, null, null, null, 1)
        }

        val media = mediaDeferred.await() ?: return@coroutineScope null
        if (com.nuvio.app.features.anilist.KaiHooks.isNonVideoMedia(media.format)) {
            log.d { "Stopping access to details page for non-video media: id=${media.id} format=${media.format}" }
            return@coroutineScope null
        }
        val arm = armDeferred.await()
        val effectiveImdbId = resolveEffectiveImdbId(media, arm.imdbId)
        val isSpecial = isSpecialAnime(media)
        val targetSeason = when {
            arm.season == 0 -> 0
            isSpecial -> 0
            else -> arm.season
        }

        val backdrop = if (!effectiveImdbId.isNullOrBlank()) {
            "https://images.metahub.space/background/medium/$effectiveImdbId/img"
        } else media.bannerImage

        val logo = if (!isSpecial && targetSeason != 0 && !effectiveImdbId.isNullOrBlank()) {
            "https://images.metahub.space/logo/medium/$effectiveImdbId/img"
        } else null

        AnimeStreamIdManager.registerOptions(
            anilistId = anilistId,
            imdbId = effectiveImdbId,
            kitsuId = arm.kitsuId,
            tmdbId = arm.tmdbId,
            season = targetSeason,
        )

        buildBaseMetaFromAnilistMedia(
            media = media,
            backdrop = backdrop,
            logo = logo,
            season = targetSeason,
            imdbId = effectiveImdbId,
            requestedId = rawId,
        )
    }

    suspend fun enrichAnimeForMetaScreen(
        meta: MetaDetails,
        onUpdate: suspend ((MetaDetails) -> MetaDetails) -> Unit,
    ): MetaDetails = coroutineScope {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(meta.id) ?: return@coroutineScope meta
        val token = AnilistAuthRepository.token.value
        val cachedMedia = AnilistApi.getCachedMedia(anilistId)

        // 1. Live full media in background
        val mediaDeferred = async {
            if (cachedMedia != null && cachedMedia.isFullDetails) {
                cachedMedia
            } else {
                runCatching {
                    withTimeoutOrNull(3000L) {
                        AnilistApi.fetchMediaById(anilistId, token = token)
                    }
                }.getOrNull() ?: cachedMedia
            }
        }

        // 2. ARM mapping in background
        val armDeferred = async {
            val cachedArm = armMappingCache[anilistId]
            cachedArm ?: runCatching {
                withTimeoutOrNull(2000L) { resolveArmMapping(anilistId) }
            }.getOrNull() ?: ArmMapping(null, null, null, null, null, 1)
        }

        // 3. MAL score in background
        val malDeferred = async {
            val media = cachedMedia ?: mediaDeferred.await()
            val idMal = media?.idMal
            if (idMal != null) {
                runCatching {
                    withTimeoutOrNull(3000L) { AnilistApi.fetchMalMetadata(idMal) }
                }.getOrNull()
            } else null
        }

        // Stream media relations & recommendations as soon as media arrives
        launch {
            val media = mediaDeferred.await()
            if (media != null) {
                val recs = media.recommendations.mapNotNull { rec ->
                    val recId = rec.id
                    val isRecMovie = rec.format == "MOVIE" || rec.episodes == 1
                    val recType = if (isRecMovie) "movie" else "series"
                    val recPoster = rec.coverImage?.bestUrl
                        ?: rec.coverImage?.extraLarge
                        ?: rec.coverImage?.large
                        ?: rec.coverImage?.medium
                        ?: return@mapNotNull null
                    val recScore = if (rec.averageScore != null && rec.averageScore > 0) rec.averageScore / 10.0 else null
                    com.nuvio.app.features.home.MetaPreview(
                        id = "ani_$recId",
                        type = recType,
                        name = rec.title?.displayTitle.orEmpty(),
                        poster = recPoster,
                        banner = rec.bannerImage,
                        logo = MetaHubArtwork.getLogoUrl("ani_$recId"),
                        description = null,
                        releaseInfo = if (rec.episodes != null) "${rec.episodes} Ep" else null,
                        imdbRating = recScore?.let { "${((it * 10).toInt()) / 10.0}" },
                        anilistScore = if (rec.averageScore != null && rec.averageScore > 0) rec.averageScore.toDouble() else null,
                    )
                }

                val rels = media.relations.mapNotNull { rel ->
                    val relTitle = rel.title?.displayTitle?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val relPoster = rel.coverImage?.bestUrl
                        ?: rel.coverImage?.extraLarge
                        ?: rel.coverImage?.large
                        ?: rel.coverImage?.medium
                    val isRelMovie = rel.format == "MOVIE" || rel.episodes == 1
                    val relType = if (isRelMovie) "movie" else "series"
                    val relationLabel = when (rel.relationType?.uppercase()) {
                        "PREQUEL" -> "Prequel"
                        "SEQUEL" -> "Sequel"
                        "PARENT" -> "Parent Story"
                        "SIDE_STORY" -> "Side Story"
                        "SPIN_OFF" -> "Spin-Off"
                        "ALTERNATIVE" -> "Alternative"
                        "SUMMARY" -> "Summary"
                        "CHARACTER" -> "Character"
                        "OTHER" -> "Other"
                        else -> rel.relationType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Related"
                    }
                    com.nuvio.app.features.details.MetaRelation(
                        id = "ani_${rel.id}",
                        type = relType,
                        relationType = relationLabel,
                        title = relTitle,
                        poster = relPoster,
                        format = rel.format,
                        episodes = rel.episodes,
                        status = rel.status,
                        averageScore = rel.averageScore,
                    )
                }

                val castPersons = buildCategorizedCast(media)
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
                val primaryTrailer = if (media.trailer != null && media.trailer.id != null) {
                    listOf(
                        MetaTrailer(
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
                val cleanDescription = com.nuvio.app.core.format.cleanHtmlDescription(media.description)

                onUpdate { current ->
                    current.copy(
                        moreLikeThis = if (recs.isNotEmpty()) recs else current.moreLikeThis,
                        relations = if (rels.isNotEmpty()) rels else current.relations,
                        cast = if (castPersons.isNotEmpty()) castPersons else current.cast,
                        productionCompanies = if (animationStudios.isNotEmpty()) animationStudios else current.productionCompanies,
                        networks = if (networks.isNotEmpty()) networks else current.networks,
                        trailers = if (primaryTrailer.isNotEmpty()) primaryTrailer else current.trailers,
                        director = if (directors.isNotEmpty()) directors else current.director,
                        writer = if (writers.isNotEmpty()) writers else current.writer,
                        description = cleanDescription?.takeIf { it.isNotBlank() } ?: current.description,
                        status = media.status ?: current.status,
                    )
                }
            }
        }

        // Stream MAL score as soon as MAL API responds
        launch {
            val malMeta = malDeferred.await()
            if (malMeta?.score != null && malMeta.score > 0) {
                onUpdate { current ->
                    val existing = current.externalRatings.filterNot { it.source == "mal" }
                    val updated = existing + com.nuvio.app.features.details.MetaExternalRating("mal", malMeta.score)
                    current.copy(
                        externalRatings = updated,
                        ageRating = malMeta.ageRating ?: current.ageRating,
                    )
                }
            }
        }

        // Stream ARM mapping & Metahub backdrop & Kitsu episodes concurrently
        launch {
            val media = mediaDeferred.await()
            val arm = armDeferred.await()
            val effectiveImdbId = resolveEffectiveImdbId(media, arm.imdbId)
            val isSpecial = isSpecialAnime(media)
            val targetSeason = when {
                arm.season == 0 -> 0
                isSpecial -> 0
                else -> arm.season
            }

            val kitsuId = arm.kitsuId?.removePrefix("kitsu:")?.takeIf { it.isNotBlank() }
                ?: resolveKitsuId(anilistId, media)

            com.nuvio.app.features.anilist.streams.AnimeStreamIdManager.registerOptions(
                anilistId = anilistId,
                imdbId = effectiveImdbId,
                kitsuId = kitsuId,
                tmdbId = arm.tmdbId,
                season = targetSeason,
            )

            // Update Metahub backdrop immediately as soon as IMDb ID resolves
            if (!effectiveImdbId.isNullOrBlank()) {
                val metahubBackdrop = "https://images.metahub.space/background/medium/$effectiveImdbId/img"
                val metahubLogo = if (!isSpecial && targetSeason != 0) "https://images.metahub.space/logo/medium/$effectiveImdbId/img" else null
                onUpdate { current ->
                    current.copy(
                        background = metahubBackdrop,
                        logo = metahubLogo ?: current.logo,
                    )
                }
            }

            // Fetch Kitsu episodes and update episode list with Metahub/Kitsu thumbnails
            if (!kitsuId.isNullOrBlank() || !effectiveImdbId.isNullOrBlank()) {
                val kitsuEpisodes = if (!kitsuId.isNullOrBlank()) {
                    runCatching {
                        withTimeoutOrNull(2500L) { fetchKitsuEpisodes(kitsuId) }
                    }.getOrNull() ?: emptyMap()
                } else emptyMap()

                val hasValidImdbId = effectiveImdbId?.startsWith("tt", ignoreCase = true) == true
                val isMovie = media?.format == "MOVIE"
                val totalEpisodes = media?.episodes ?: media?.streamingEpisodes?.size?.takeIf { it > 0 } ?: kitsuEpisodes.size.takeIf { it > 0 } ?: 12
                val episodeOffset = resolveEpisodeOffset(
                    media = media,
                    targetSeason = targetSeason,
                    cinemetaVideos = emptyList(),
                )

                val updatedVideos = if (isMovie) {
                    emptyList()
                } else if (targetSeason == 0) {
                    val startsAtZero = media?.description?.contains("Includes Episode 0", ignoreCase = true) == true ||
                        (media?.description?.contains("Episode 0", ignoreCase = true) == true && media.streamingEpisodes.any { it.title?.contains("Episode 0", ignoreCase = true) == true })
                    val epRange = if (startsAtZero) (0 until totalEpisodes) else (1..totalEpisodes)

                    epRange.map { idx ->
                        val actualEpNumber = idx + episodeOffset
                        val streamingEp = if (startsAtZero) media?.streamingEpisodes?.getOrNull(idx) else media?.streamingEpisodes?.getOrNull(idx - 1)
                        val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx] ?: (if (startsAtZero && idx == 0) kitsuEpisodes[0] else null)
                        val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                            ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                            ?: if (actualEpNumber == 0) "Episode 0" else "Episode $actualEpNumber"
                        val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                        val metahubThumb = if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/0/$actualEpNumber/w780.jpg" else null
                        val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                        val videoId = resolveEpisodeVideoId(
                            anilistId = anilistId,
                            season = 0,
                            episode = actualEpNumber,
                            effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                            kitsuId = kitsuId,
                            relativeEpisode = idx,
                        )
                        MetaVideo(
                            id = videoId,
                            title = epTitle,
                            season = 0,
                            episode = actualEpNumber,
                            overview = kitsuEp?.overview,
                            thumbnail = metahubThumb ?: kitsuFallback,
                            fallbackThumbnail = kitsuFallback,
                            runtime = media?.duration,
                            released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, null, media),
                            streams = emptyList(),
                        )
                    }
                } else {
                    (1..totalEpisodes).map { epIdx ->
                        val actualEpNumber = epIdx + episodeOffset
                        val streamingEp = media?.streamingEpisodes?.getOrNull(epIdx - 1)
                        val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[epIdx]
                        val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                            ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                        val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                        val metahubThumb = if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg" else null
                        val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                        val videoId = resolveEpisodeVideoId(
                            anilistId = anilistId,
                            season = targetSeason,
                            episode = actualEpNumber,
                            effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                            kitsuId = kitsuId,
                            relativeEpisode = epIdx,
                        )

                        MetaVideo(
                            id = videoId,
                            title = epTitle,
                            season = targetSeason,
                            episode = actualEpNumber,
                            overview = kitsuEp?.overview,
                            thumbnail = metahubThumb ?: kitsuFallback,
                            fallbackThumbnail = kitsuFallback,
                            runtime = media?.duration,
                            released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, null, media),
                            streams = emptyList(),
                        )
                    }
                }

                val defaultVidId = if (isMovie) {
                    val fallbackId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId"
                        else if (!effectiveImdbId.isNullOrBlank()) effectiveImdbId
                        else "anilist:$anilistId"
                    AnimeStreamIdManager.resolvePlaybackVideoId(
                        parentMetaId = "ani_$anilistId",
                        season = 1,
                        episode = 1,
                        isMovie = true,
                        fallbackVideoId = fallbackId,
                    )
                } else updatedVideos.firstOrNull()?.id

                onUpdate { current ->
                    current.copy(
                        videos = updatedVideos,
                        defaultVideoId = defaultVidId ?: current.defaultVideoId,
                    )
                }
            }
        }

        meta
    }

    data class ArmMapping(
        val imdbId: String?,
        val kitsuId: String?,
        val tmdbId: Int?,
        val tvdbId: String?,
        val malId: Int? = null,
        val season: Int = 1,
    )

    private val armMappingCache = mutableMapOf<Int, ArmMapping>()
    private val kitsuIdCache = mutableMapOf<String, String>()

    suspend fun resolveArmMapping(anilistId: Int): ArmMapping {
        armMappingCache[anilistId]?.let { return it }

        return withTimeoutOrNull(2500L) {
            runCatching {
                val url = "https://arm.haglund.dev/api/v2/ids?source=anilist&id=$anilistId"
                val text = httpGetText(url) ?: return@runCatching ArmMapping(null, null, null, null, null, 1)
                val obj = json.parseToJsonElement(text).asJsonObjectOrNull() ?: return@runCatching ArmMapping(null, null, null, null, null, 1)
                val imdb = obj["imdb"].asStringOrNull()
                val kitsu = obj["kitsu"].asStringOrNull()
                val tmdb = obj["themoviedb"].asIntOrNull()
                val tvdb = obj["thetvdb"].asStringOrNull() ?: obj["thetvdb"].asIntOrNull()?.toString()
                val mal = obj["myanimelist"].asIntOrNull()
                val season = obj["thetvdb-season"].asIntOrNull()
                    ?: obj["themoviedb-season"].asIntOrNull()
                    ?: 1

                val mapping = ArmMapping(
                    imdbId = imdb,
                    kitsuId = kitsu,
                    tmdbId = tmdb,
                    tvdbId = tvdb,
                    malId = mal,
                    season = if (season >= 0) season else 1,
                )
                armMappingCache[anilistId] = mapping
                mapping
            }.getOrDefault(ArmMapping(null, null, null, null, null, 1))
        } ?: ArmMapping(null, null, null, null, null, 1)
    }

    suspend fun resolveEffectiveImdbId(media: AnilistMedia?, directImdbId: String?): String? {
        if (!directImdbId.isNullOrBlank()) return directImdbId
        if (media == null) return null

        // A long-running mainline TV show (e.g. One Piece, Detective Conan) with direct episodes should never borrow
        // an ID from external spin-off relations like one-shot ONA prequels (e.g. Monsters 103 Mercies).
        val isMainlineTvSeries = media.format == "TV" && (media.episodes == null || media.episodes > 24)
        if (isMainlineTvSeries) return null

        // Only look at PARENT or valid PREQUEL relations (e.g. for Season 2 / cour 2 referencing Season 1)
        // NEVER look at SPIN_OFF, SIDE_STORY, ALTERNATIVE, or OTHER which can cross-contaminate unrelated spin-offs!
        val priorityTypes = listOf("PARENT", "PREQUEL")
        val candidateRelations = media.relations.filter { rel ->
            val type = rel.relationType?.uppercase()
            if (type !in priorityTypes) return@filter false
            // If checking PREQUEL, ensure the candidate is not an ONA/MOVIE/SPECIAL one-shot when current is a TV series
            if (type == "PREQUEL") {
                val relFormat = rel.format?.uppercase()
                if (media.format == "TV" && relFormat in listOf("ONA", "MOVIE", "SPECIAL", "OVA", "MUSIC")) {
                    return@filter false
                }
            }
            true
        }
        for (rel in candidateRelations) {
            val mapped = resolveArmMapping(rel.id).imdbId
            if (!mapped.isNullOrBlank()) return mapped
        }

        return null
    }

    suspend fun adaptCinemetaForAnilist(
        cinemetaMeta: MetaDetails,
        rawId: String,
    ): MetaDetails = withContext(Dispatchers.Default) {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(rawId) ?: return@withContext cinemetaMeta
        val token = AnilistAuthRepository.token.value
        val media: AnilistMedia? = AnilistApi.getCachedMedia(anilistId)?.takeIf { it.isFullDetails }
            ?: AnilistApi.fetchMediaById(anilistId, token = token)
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
        val effectiveImdbId = resolveEffectiveImdbId(
            media = media,
            directImdbId = mapping.imdbId ?: cinemetaMeta.id.takeIf { it.startsWith("tt") }
        )

        val isMovie = media?.format == "MOVIE"

        val recommendations = media?.recommendations?.mapNotNull { rec ->
            val recId = rec.id
            val isRecMovie = rec.format == "MOVIE" || rec.episodes == 1
            val recType = if (isRecMovie) "movie" else "series"
            val recPoster = rec.coverImage?.extraLarge ?: rec.coverImage?.large ?: return@mapNotNull null
            val recScore = if (rec.averageScore != null && rec.averageScore > 0) rec.averageScore / 10.0 else null
            com.nuvio.app.features.home.MetaPreview(
                id = "ani_$recId",
                type = recType,
                name = rec.title?.displayTitle.orEmpty(),
                poster = recPoster,
                banner = rec.bannerImage,
                logo = MetaHubArtwork.getLogoUrl("ani_$recId"),
                description = null,
                releaseInfo = if (rec.episodes != null) "${rec.episodes} Ep" else null,
                imdbRating = recScore?.let { "${((it * 10).toInt()) / 10.0}" },
                anilistScore = if (rec.averageScore != null && rec.averageScore > 0) rec.averageScore.toDouble() else null,
            )
        }.orEmpty()

        val kitsuId = mapping.kitsuId?.removePrefix("kitsu:")?.takeIf { it.isNotBlank() }
            ?: resolveKitsuId(anilistId, media)

        val kitsuEpisodes = if (!kitsuId.isNullOrBlank()) {
            fetchKitsuEpisodes(kitsuId)
        } else emptyMap()

        val releaseYear = when {
            media?.startDateYear != null -> "${media.startDateYear}"
            else -> cinemetaMeta.releaseInfo
        }

        val cleanDesc = com.nuvio.app.core.format.cleanHtmlDescription(media?.description)
            ?: cinemetaMeta.description

        val anilistCast = media?.let { buildCategorizedCast(it) }.orEmpty()

        val animationStudios = media?.studios?.filter { it.isAnimationStudio }?.mapNotNull { studio ->
            studio.name?.takeIf { it.isNotBlank() }?.let { name ->
                com.nuvio.app.features.details.MetaCompany(
                    name = name,
                    logo = AnimeStudioLogos.findLogo(name),
                )
            }
        }.orEmpty()

        val networks = media?.studios?.filter { !it.isAnimationStudio }?.mapNotNull { studio ->
            studio.name?.takeIf { it.isNotBlank() }?.let { name ->
                com.nuvio.app.features.details.MetaCompany(
                    name = name,
                    logo = AnimeStudioLogos.findLogo(name),
                )
            }
        }.orEmpty()

        val relations = media?.relations?.mapNotNull { rel ->
            val relTitle = rel.title?.displayTitle?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val relPoster = rel.coverImage?.extraLarge ?: rel.coverImage?.large
            val isRelMovie = rel.format == "MOVIE" || rel.episodes == 1
            val relType = if (isRelMovie) "movie" else "series"
            val relationLabel = when (rel.relationType?.uppercase()) {
                "PREQUEL" -> "Prequel"
                "SEQUEL" -> "Sequel"
                "PARENT" -> "Parent Story"
                "SIDE_STORY" -> "Side Story"
                "SPIN_OFF" -> "Spin-Off"
                "ALTERNATIVE" -> "Alternative"
                "SUMMARY" -> "Summary"
                "CHARACTER" -> "Character"
                "OTHER" -> "Other"
                else -> rel.relationType?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Related"
            }
            com.nuvio.app.features.details.MetaRelation(
                id = "ani_${rel.id}",
                type = relType,
                relationType = relationLabel,
                title = relTitle,
                poster = relPoster,
                format = rel.format,
                episodes = rel.episodes,
                status = rel.status,
                averageScore = rel.averageScore,
            )
        }.orEmpty()

        val trailers = if (media?.trailer != null && media.trailer.id != null) {
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
        } else cinemetaMeta.trailers

        if (isMovie) {
            return@withContext cinemetaMeta.copy(
                id = rawId,
                type = "movie",
                name = media?.title?.displayTitle ?: cinemetaMeta.name,
                poster = anilistPoster,
                description = cleanDesc,
                releaseInfo = releaseYear,
                status = media?.status ?: cinemetaMeta.status,
                lastAirDate = media?.endDateYear?.toString() ?: releaseYear,
                background = cinemetaMeta.background ?: "https://images.metahub.space/background/medium/$effectiveImdbId/img",
                logo = cinemetaMeta.logo ?: "https://images.metahub.space/logo/medium/$effectiveImdbId/img",
                cast = if (anilistCast.isNotEmpty()) anilistCast else cinemetaMeta.cast,
                productionCompanies = if (animationStudios.isNotEmpty()) animationStudios else cinemetaMeta.productionCompanies,
                networks = if (networks.isNotEmpty()) networks else cinemetaMeta.networks,
                relations = relations,
                trailers = trailers,
                defaultVideoId = AnimeStreamIdManager.resolvePlaybackVideoId(
                    parentMetaId = "ani_$anilistId",
                    season = 1,
                    episode = 1,
                    isMovie = true,
                    fallbackVideoId = if (!kitsuId.isNullOrBlank()) "kitsu:$kitsuId" else if (!effectiveImdbId.isNullOrBlank()) effectiveImdbId else "anilist:$anilistId",
                ),
                moreLikeThis = recommendations.ifEmpty { cinemetaMeta.moreLikeThis },
            )
        }

        val episodeOffset = resolveEpisodeOffset(
            media = media,
            targetSeason = targetSeason,
            cinemetaVideos = cinemetaMeta.videos,
        )

        val hasValidImdbId = effectiveImdbId?.startsWith("tt", ignoreCase = true) == true
        val fallbackThumb = media?.bannerImage ?: media?.coverImage?.bestUrl

        // For seasons, isolate and map that season's episodes cleanly using Kitsu IDs
        val seasonVideos = if (targetSeason == 0 && cinemetaMeta.videos.any { it.season == 0 }) {
            val specialVideos = cinemetaMeta.videos.filter { it.season == 0 }
            val isSingleSpecial = (media?.episodes ?: 1) == 1 && specialVideos.size <= 1
            if (isSingleSpecial) {
                val matchedSpecial = findSpecialMatch(media, specialVideos) ?: specialVideos.first()
                val epNum = matchedSpecial.episode ?: 1
                val videoId = resolveEpisodeVideoId(
                    anilistId = anilistId,
                    season = 0,
                    episode = epNum,
                    effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                    kitsuId = kitsuId,
                )
                val kitsuEp = kitsuEpisodes[epNum] ?: (if (epNum == 0) kitsuEpisodes[0] else null)
                val streamingEp = media?.streamingEpisodes?.firstOrNull()
                val epThumbnail = matchedSpecial.thumbnail?.takeIf { it.isNotBlank() }
                    ?: kitsuEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: (if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/0/$epNum/w780.jpg" else null)
                    ?: fallbackThumb
                listOf(
                    matchedSpecial.copy(
                        id = videoId,
                        season = 0,
                        episode = epNum,
                        title = matchedSpecial.title?.takeIf { it.isNotBlank() } ?: media?.title?.displayTitle ?: "Special",
                        thumbnail = epThumbnail,
                        released = resolveEpisodeAirDate(epNum, kitsuEp?.airdate, matchedSpecial.released, media),
                    )
                )
            } else {
                // Multi-episode Special / ONA / Mini-Series (e.g. Sousou no Frieren: ●● no Mahou with 11 episodes)
                val totalEps = media?.episodes ?: specialVideos.size.takeIf { it > 0 } ?: kitsuEpisodes.size.takeIf { it > 0 } ?: 1
                (1..totalEps).map { idx ->
                    val actualEpNumber = idx + episodeOffset
                    val matchingCinemetaEp = specialVideos.firstOrNull { it.episode == actualEpNumber } ?: specialVideos.firstOrNull { it.episode == idx }
                    val videoId = resolveEpisodeVideoId(
                        anilistId = anilistId,
                        season = 0,
                        episode = actualEpNumber,
                        effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                        kitsuId = kitsuId,
                        relativeEpisode = idx,
                    )
                    val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx]
                    val streamingEp = media?.streamingEpisodes?.getOrNull(idx - 1)
                    val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                        ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                        ?: matchingCinemetaEp?.title?.takeIf { it.isNotBlank() }
                        ?: "Episode $actualEpNumber"
                    val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                    val metahubThumb = matchingCinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                        ?: (if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/0/$actualEpNumber/w780.jpg" else null)
                    val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }

                    MetaVideo(
                        id = videoId,
                        season = 0,
                        episode = actualEpNumber,
                        title = epTitle,
                        thumbnail = metahubThumb ?: kitsuFallback ?: fallbackThumb,
                        fallbackThumbnail = kitsuFallback,
                        overview = kitsuEp?.overview ?: matchingCinemetaEp?.overview,
                        runtime = media?.duration,
                        released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, matchingCinemetaEp?.released, media),
                    )
                }
            }
        } else if (targetSeason == 0) {
            val totalEps = media?.episodes ?: media?.streamingEpisodes?.size?.takeIf { it > 0 } ?: kitsuEpisodes.size.takeIf { it > 0 } ?: 1
            val startsAtZero = media?.description?.contains("Includes Episode 0", ignoreCase = true) == true ||
                (media?.description?.contains("Episode 0", ignoreCase = true) == true && media?.streamingEpisodes?.any { it.title?.contains("Episode 0", ignoreCase = true) == true } == true)
            val epRange = if (startsAtZero) (0 until totalEps) else (1..totalEps)

            epRange.map { idx ->
                val actualEpNumber = idx + episodeOffset
                val streamingEp = if (startsAtZero) media?.streamingEpisodes?.getOrNull(idx) else media?.streamingEpisodes?.getOrNull(idx - 1)
                val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx] ?: (if (startsAtZero && idx == 0) kitsuEpisodes[0] else null)
                val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                    ?: if (actualEpNumber == 0) "Episode 0" else "Episode $actualEpNumber"
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val metahubThumb = if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/0/$actualEpNumber/w780.jpg" else null
                val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail?.takeIf { it.isNotBlank() }
                val videoId = resolveEpisodeVideoId(
                    anilistId = anilistId,
                    season = 0,
                    episode = actualEpNumber,
                    effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                    kitsuId = kitsuId,
                    relativeEpisode = idx,
                )
                MetaVideo(
                    id = videoId,
                    season = 0,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = metahubThumb ?: kitsuFallback ?: fallbackThumb,
                    fallbackThumbnail = kitsuFallback,
                    overview = kitsuEp?.overview,
                    runtime = media?.duration,
                    released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, null, media),
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
                val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val metahubThumb = cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: (if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg" else null)
                val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail
                val videoId = resolveEpisodeVideoId(
                    anilistId = anilistId,
                    season = targetSeason,
                    episode = actualEpNumber,
                    effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                    kitsuId = kitsuId,
                    relativeEpisode = idx,
                )
                MetaVideo(
                    id = videoId,
                    season = targetSeason,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = metahubThumb ?: kitsuFallback ?: fallbackThumb,
                    fallbackThumbnail = kitsuFallback,
                    overview = kitsuEp?.overview ?: cinemetaEp?.overview,
                    released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, cinemetaEp?.released, media),
                    streams = cinemetaEp?.streams.orEmpty(),
                    runtime = media?.duration ?: cinemetaEp?.runtime,
                )
            }
        } else if (targetSeason > 1) {
            val epCount = media?.episodes ?: kitsuEpisodes.size.takeIf { it > 0 } ?: 12
            (1..epCount).map { idx ->
                val actualEpNumber = idx + episodeOffset
                val streamingEp = media?.streamingEpisodes?.getOrNull(idx - 1)
                val kitsuEp = kitsuEpisodes[actualEpNumber] ?: kitsuEpisodes[idx]
                val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val metahubThumb = if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/$targetSeason/$actualEpNumber/w780.jpg" else null
                val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail
                val videoId = resolveEpisodeVideoId(
                    anilistId = anilistId,
                    season = targetSeason,
                    episode = actualEpNumber,
                    effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                    kitsuId = kitsuId,
                    relativeEpisode = idx,
                )
                MetaVideo(
                    id = videoId,
                    season = targetSeason,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = metahubThumb ?: kitsuFallback ?: fallbackThumb,
                    fallbackThumbnail = kitsuFallback,
                    overview = kitsuEp?.overview,
                    runtime = media?.duration,
                    released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, null, media),
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
                val rawTitle = kitsuEp?.title?.takeIf { it.isNotBlank() }
                    ?: cinemetaEp?.title?.takeIf { it.isNotBlank() }
                    ?: streamingEp?.title?.takeIf { it.isNotBlank() }
                val epTitle = cleanEpisodeTitle(rawTitle, actualEpNumber)
                val metahubThumb = cinemetaEp?.thumbnail?.takeIf { it.isNotBlank() }
                    ?: (if (hasValidImdbId) "https://episodes.metahub.space/$effectiveImdbId/1/$actualEpNumber/w780.jpg" else null)
                val kitsuFallback = kitsuEp?.thumbnail?.takeIf { it.isNotBlank() } ?: streamingEp?.thumbnail
                val videoId = resolveEpisodeVideoId(
                    anilistId = anilistId,
                    season = 1,
                    episode = actualEpNumber,
                    effectiveImdbId = if (hasValidImdbId) effectiveImdbId else null,
                    kitsuId = kitsuId,
                    relativeEpisode = idx,
                )
                MetaVideo(
                    id = videoId,
                    season = 1,
                    episode = actualEpNumber,
                    title = epTitle,
                    thumbnail = metahubThumb ?: kitsuFallback ?: fallbackThumb,
                    fallbackThumbnail = kitsuFallback,
                    overview = kitsuEp?.overview ?: cinemetaEp?.overview,
                    released = resolveEpisodeAirDate(actualEpNumber, kitsuEp?.airdate, cinemetaEp?.released, media),
                    streams = cinemetaEp?.streams.orEmpty(),
                    runtime = media?.duration ?: cinemetaEp?.runtime,
                )
            }
        }

        val nextAiringCountdown = media?.nextAiringEpisode?.let { nextEp ->
            val epNum = nextEp.episode ?: return@let null
            val timeUntil = nextEp.timeUntilAiring
            if (timeUntil != null && timeUntil > 0) {
                val days = timeUntil / 86400
                val hours = (timeUntil % 86400) / 3600
                val mins = (timeUntil % 3600) / 60
                val timeFormatted = when {
                    days > 0 -> "${days}d ${hours}h"
                    hours > 0 -> "${hours}h ${mins}m"
                    else -> "${mins}m"
                }
                "Ep $epNum in $timeFormatted"
            } else {
                "Ep $epNum Airing Soon"
            }
        }

        val backdrop = if (!effectiveImdbId.isNullOrBlank()) {
            "https://images.metahub.space/background/medium/$effectiveImdbId/img"
        } else {
            cinemetaMeta.background ?: media?.bannerImage ?: cinemetaMeta.poster
        }

        cinemetaMeta.copy(
            id = rawId,
            name = media?.title?.displayTitle ?: cinemetaMeta.name,
            poster = anilistPoster,
            description = cleanDesc,
            releaseInfo = releaseYear,
            status = media?.status ?: cinemetaMeta.status,
            lastAirDate = media?.endDateYear?.toString() ?: releaseYear,
            background = backdrop,
            logo = if (isSpecial || targetSeason == 0) null else (cinemetaMeta.logo ?: "https://images.metahub.space/logo/medium/$effectiveImdbId/img"),
            cast = if (anilistCast.isNotEmpty()) anilistCast else cinemetaMeta.cast,
            productionCompanies = if (animationStudios.isNotEmpty()) animationStudios else cinemetaMeta.productionCompanies,
            networks = if (networks.isNotEmpty()) networks else cinemetaMeta.networks,
            relations = relations,
            trailers = if (trailers.isNotEmpty()) trailers else cinemetaMeta.trailers,
            nextAiringEpisode = nextAiringCountdown,
            videos = seasonVideos,
            moreLikeThis = recommendations.ifEmpty { cinemetaMeta.moreLikeThis },
        )
    }

    private fun buildCategorizedCast(media: AnilistMedia): List<com.nuvio.app.features.details.MetaPerson> =
        buildList {
            // 1. Characters in the show
            media.characters.forEach { char ->
                val charName = char.name ?: return@forEach
                val charRole = if (char.role?.equals("MAIN", ignoreCase = true) == true) "Main Character" else "Supporting Character"
                add(
                    com.nuvio.app.features.details.MetaPerson(
                        name = charName,
                        role = charRole,
                        photo = char.image,
                        tmdbId = char.id,
                        category = "Characters",
                    )
                )
            }

            // 2. Japanese Voice Cast
            media.characters.forEach { char ->
                val va = char.japaneseVoiceActor ?: return@forEach
                val vaName = va.name ?: return@forEach
                val role = if (char.name != null) "${char.name} (VA)" else "Voice Actor"
                add(
                    com.nuvio.app.features.details.MetaPerson(
                        name = vaName,
                        role = role,
                        photo = va.image,
                        tmdbId = va.id,
                        category = "Japanese Cast",
                    )
                )
            }

            // 3. English Dub Voice Cast (if available)
            media.characters.forEach { char ->
                val va = char.englishVoiceActor ?: return@forEach
                val vaName = va.name ?: return@forEach
                val role = if (char.name != null) "${char.name} (VA)" else "Voice Actor"
                add(
                    com.nuvio.app.features.details.MetaPerson(
                        name = vaName,
                        role = role,
                        photo = va.image,
                        tmdbId = va.id,
                        category = "English Cast",
                    )
                )
            }

            // 4. Staff & Production Crew (Deduplicated with combined roles)
            val staffMap = linkedMapOf<String, com.nuvio.app.features.details.MetaPerson>()
            media.staff.forEach { st ->
                val name = st.name ?: return@forEach
                val key = st.id?.toString() ?: name
                val rawRole = st.role?.trim()?.takeIf { it.isNotBlank() } ?: "Staff"
                val existing = staffMap[key]
                if (existing == null) {
                    staffMap[key] = com.nuvio.app.features.details.MetaPerson(
                        name = name,
                        role = rawRole,
                        photo = st.image,
                        tmdbId = st.id,
                        category = "Staff & Crew",
                    )
                } else {
                    val currentRoles = existing.role.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val mergedRoles = if (!currentRoles.contains(rawRole)) {
                        (currentRoles + rawRole).joinToString(", ")
                    } else {
                        existing.role
                    }
                    staffMap[key] = existing.copy(
                        role = mergedRoles,
                        photo = existing.photo ?: st.image,
                    )
                }
            }
            addAll(staffMap.values)
        }

    private fun resolveEpisodeAirDate(
        actualEpNumber: Int,
        kitsuAirdate: String?,
        cinemetaReleased: String?,
        media: AnilistMedia?,
    ): String? {
        // 1. Genuine airdate from Kitsu
        if (!kitsuAirdate.isNullOrBlank()) return kitsuAirdate

        // 2. Genuine release date from Cinemeta
        if (!cinemetaReleased.isNullOrBlank()) return cinemetaReleased

        // 3. AniList airing schedule (exact epoch timestamp for each episode)
        val airingAt = media?.airingSchedule?.get(actualEpNumber)
        if (airingAt != null && airingAt > 0) {
            val isoDate = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.localIsoDateAtEpochMs(airingAt * 1000L)
            if (!isoDate.isNullOrBlank()) return isoDate
        }

        // 4. AniList startDate calculation (exact broadcast schedule for weekly airing series)
        val startYear = media?.startDateYear
        val startMonth = media?.startDateMonth
        val startDay = media?.startDateDay
        if (startYear != null && startMonth != null && startDay != null && startYear in 1900..2100 && startMonth in 1..12 && startDay in 1..31) {
            val formattedStartDate = "${startYear.toString().padStart(4, '0')}-${startMonth.toString().padStart(2, '0')}-${startDay.toString().padStart(2, '0')}"
            if (actualEpNumber <= 1) {
                return formattedStartDate
            } else {
                val startEpochMs = com.nuvio.app.core.time.isoEpochDay(formattedStartDate) * 86_400_000L
                val epEpochMs = startEpochMs + (actualEpNumber - 1) * 7L * 86_400_000L
                val epIsoDate = com.nuvio.app.core.time.EpisodeReleaseDatePlatform.localIsoDateAtEpochMs(epEpochMs)
                if (!epIsoDate.isNullOrBlank()) return epIsoDate
            }
        }

        return null
    }

    private suspend fun resolveKitsuId(anilistId: Int, media: AnilistMedia? = null, rawTitle: String? = null): String? {
        val displayTitle = media?.title?.displayTitle ?: rawTitle.orEmpty()
        val cacheKey = "$anilistId:$displayTitle"
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
            val titlesToTry = listOfNotNull(
                media?.title?.romaji,
                media?.title?.english,
                media?.title?.displayTitle,
                rawTitle,
            ).map { it.replace(Regex("[●★☆【】・:—–\\-_]"), " ").replace(Regex("\\s+"), " ").trim() }
                .filter { it.isNotBlank() }
                .distinct()

            for (tryTitle in titlesToTry) {
                val encodedQuery = tryTitle.encodeUnsafeHttpUrlCharacters()
                val kitsuSearchUrl = "https://kitsu.io/api/edge/anime?filter%5Btext%5D=$encodedQuery&page%5Blimit%5D=1"
                val searchRes = httpGetText(kitsuSearchUrl) ?: continue
                val sObj = json.parseToJsonElement(searchRes).asJsonObjectOrNull() ?: continue
                val id = sObj["data"].asJsonArrayOrNull()?.firstOrNull()?.asJsonObjectOrNull()?.get("id").asStringOrNull()
                if (!id.isNullOrBlank()) {
                    kitsuIdCache[cacheKey] = id
                    return@runCatching id
                }
            }
            null
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
        val result = mutableMapOf<Int, KitsuEpisodeData>()

        suspend fun fetchPage(offset: Int): Int {
            val url = "https://kitsu.io/api/edge/anime/$cleanKitsuId/episodes?page%5Blimit%5D=20&page%5Boffset%5D=$offset"
            val response = runCatching {
                httpGetTextWithHeaders(
                    url = url,
                    headers = mapOf(
                        "Accept" to "application/vnd.api+json, application/json",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    ),
                )
            }.getOrNull() ?: httpGetText(url)
            if (response.isNullOrBlank()) return 0
            val root = runCatching { json.parseToJsonElement(response) }.getOrNull() ?: return 0
            val dataArray = root.asJsonObjectOrNull()?.get("data").asJsonArrayOrNull() ?: return 0

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
                    ?: thumbnailObj?.get("large").asStringOrNull()
                    ?: thumbnailObj?.get("medium").asStringOrNull()
                    ?: thumbnailObj?.get("small").asStringOrNull()
                    ?: thumbnailObj?.get("tiny").asStringOrNull()
                val airdate = attrs["airdate"].asStringOrNull() ?: attrs["released"].asStringOrNull()

                result[epNum] = KitsuEpisodeData(
                    title = epTitle,
                    overview = overview,
                    thumbnail = thumbnail,
                    airdate = airdate,
                )
            }
            return dataArray.size
        }

        val count = fetchPage(0)
        if (count == 20) {
            fetchPage(20)
        }

        result
    }.getOrElse { emptyMap() }

    internal suspend fun getMediaOffset(media: AnilistMedia): Int {
        val arm = resolveArmMapping(media.id)
        return resolveEpisodeOffset(media, arm.season, emptyList())
    }

    fun getCachedEpisodeOffset(anilistId: Int): Int {
        episodeOffsetCache[anilistId]?.let { return it }
        val media = AnilistApi.getCachedMedia(anilistId)
            ?: AnilistTrackerCoordinator.getCachedMedia(anilistId)
        if (media != null && media.relations.isNotEmpty()) {
            val arm = armMappingCache[anilistId]
            val targetSeason = arm?.season ?: 1
            if (targetSeason > 0) {
                var sum = 0
                val prequels = media.relations.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
                for (prequel in prequels) {
                    val prequelArm = armMappingCache[prequel.id]
                    if (prequelArm != null && prequelArm.season == targetSeason) {
                        val pMedia = AnilistApi.getCachedMedia(prequel.id)
                        val eps = pMedia?.episodes ?: 0
                        sum += eps
                    }
                }
                if (sum > 0) {
                    episodeOffsetCache[anilistId] = sum
                    return sum
                }
            }
        }
        return 0
    }

    private suspend fun resolveEpisodeOffset(
        media: AnilistMedia?,
        targetSeason: Int,
        cinemetaVideos: List<MetaVideo>,
    ): Int {
        if (media == null || targetSeason <= 0) return 0

        // 1. Dynamic ARM Season-Aware Prequel Summation
        val armOffset = collectPrequelChainBySeason(media.relations, targetSeason)
        if (armOffset > 0) {
            episodeOffsetCache[media.id] = armOffset
            return armOffset
        }

        // 2. Secondary Heuristic: Title Regex Part/Cour Summation
        val title = (media.title?.displayTitle.orEmpty() + " " + media.title?.romaji.orEmpty() + " " + media.title?.english.orEmpty())
        val partNum = extractPartNumber(title)
        if (partNum <= 1) {
            episodeOffsetCache[media.id] = 0
            return 0
        }

        // Traverse all prequels in the chain
        val traversed = collectPrequelChain(media.relations, title)
        val accountedParts = traversed.map { it.partNumber }.toSet()
        var totalOffset = traversed.sumOf { it.episodes }

        // If some earlier parts were not reached in relations (e.g. shallow API relations cache),
        // fill in missing parts using the closest known prequel's episode count
        val missingParts = (1 until partNum).filter { it !in accountedParts }
        if (missingParts.isNotEmpty()) {
            val avgPrequelEps = traversed.firstOrNull { it.episodes > 0 }?.episodes
                ?: media.episodes
                ?: (cinemetaVideos.count { it.season == targetSeason } / partNum).coerceAtLeast(11)
            totalOffset += missingParts.size * avgPrequelEps
        }

        episodeOffsetCache[media.id] = totalOffset
        return totalOffset
    }

    private suspend fun collectPrequelChainBySeason(
        relations: List<AnilistRelation>,
        targetSeason: Int,
        visitedIds: MutableSet<Int> = mutableSetOf(),
    ): Int {
        var sum = 0
        val prequels = relations.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
        for (prequel in prequels) {
            if (!visitedIds.add(prequel.id)) continue
            val prequelArm = resolveArmMapping(prequel.id)
            if (prequelArm.season == targetSeason) {
                val cachedMedia = AnilistApi.getCachedMedia(prequel.id)
                val eps = prequel.episodes
                    ?: cachedMedia?.episodes
                    ?: runCatching { AnilistApi.fetchMediaById(prequel.id)?.episodes }.getOrNull()
                    ?: 12
                sum += eps

                val nestedRelations = if (prequel.relations.isNotEmpty()) {
                    prequel.relations
                } else {
                    cachedMedia?.relations?.takeIf { it.isNotEmpty() }
                        ?: runCatching { AnilistApi.fetchMediaById(prequel.id)?.relations }.getOrNull().orEmpty()
                }
                if (nestedRelations.isNotEmpty()) {
                    sum += collectPrequelChainBySeason(nestedRelations, targetSeason, visitedIds)
                }
            }
        }
        return sum
    }

    private data class PrequelPartInfo(val partNumber: Int, val episodes: Int)

    private fun collectPrequelChain(relations: List<AnilistRelation>, currentTitle: String): List<PrequelPartInfo> {
        val result = mutableListOf<PrequelPartInfo>()
        val prequels = relations.filter { it.relationType.equals("PREQUEL", ignoreCase = true) }
        val currentPart = extractPartNumber(currentTitle)

        for (prequel in prequels) {
            val pTitle = (prequel.title?.displayTitle.orEmpty() + " " + prequel.title?.romaji.orEmpty() + " " + prequel.title?.english.orEmpty())
            val prequelPart = extractPartNumber(pTitle)
            if (currentPart > 1 && (prequelPart < currentPart || (prequelPart == 1 && currentPart > 1))) {
                val cachedMedia = AnilistApi.getCachedMedia(prequel.id)
                val eps = prequel.episodes ?: cachedMedia?.episodes ?: 0
                result.add(PrequelPartInfo(partNumber = prequelPart, episodes = eps))

                val nestedRelations = if (prequel.relations.isNotEmpty()) {
                    prequel.relations
                } else {
                    cachedMedia?.relations.orEmpty()
                }

                if (nestedRelations.isNotEmpty()) {
                    result.addAll(collectPrequelChain(nestedRelations, pTitle.ifEmpty { currentTitle }))
                }
            }
        }
        return result
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

    fun isSpecialAnime(media: AnilistMedia?): Boolean {
        if (media == null) return false
        val format = media.format?.uppercase()
        if (format == "TV" || format == "TV_SHORT" || format == "MOVIE") return false
        if (format == "SPECIAL" || format == "OVA") return true
        val titleText = (
            media.title?.displayTitle.orEmpty() + " " +
            media.title?.romaji.orEmpty() + " " +
            media.title?.english.orEmpty()
        ).lowercase()
        val descText = media.description.orEmpty().lowercase()

        return titleText.contains("special") ||
            titleText.contains("mini anime") ||
            titleText.contains("chibi") ||
            titleText.contains("parody") ||
            titleText.contains("picture drama") ||
            titleText.contains("omake") ||
            titleText.contains("bonus") ||
            titleText.contains("short") ||
            titleText.contains("sp") ||
            descText.contains("chibi short") ||
            descText.contains("mini anime") ||
            descText.contains("batch of the") ||
            (format == "ONA" && (
                (media.duration != null && media.duration <= 10) ||
                (media.episodes != null && media.episodes <= 13 && (media.duration == null || media.duration <= 15))
            ))
    }

    private fun resolveEpisodeVideoId(
        anilistId: Int,
        season: Int,
        episode: Int,
        effectiveImdbId: String?,
        kitsuId: String?,
        isMovie: Boolean = false,
        relativeEpisode: Int = episode,
    ): String {
        return AnimeStreamIdManager.resolvePlaybackVideoId(
            parentMetaId = "ani_$anilistId",
            season = season,
            episode = episode,
            isMovie = isMovie,
            fallbackVideoId = if (!effectiveImdbId.isNullOrBlank()) {
                if (isMovie) effectiveImdbId else "$effectiveImdbId:$season:$episode"
            } else if (!kitsuId.isNullOrBlank()) {
                val clean = kitsuId.removePrefix("kitsu:")
                if (isMovie) "kitsu:$clean" else "kitsu:$clean:$relativeEpisode"
            } else {
                if (isMovie) "anilist:$anilistId" else "anilist:$anilistId:$relativeEpisode"
            },
            relativeEpisode = relativeEpisode,
        )
    }
}
