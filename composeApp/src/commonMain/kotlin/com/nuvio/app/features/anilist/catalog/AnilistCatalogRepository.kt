package com.nuvio.app.features.anilist.catalog

import co.touchlab.kermit.Logger
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistAuthStorage
import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistMedia
import com.nuvio.app.features.anilist.AnilistMediaListStatus
import com.nuvio.app.features.catalog.CatalogPage
import com.nuvio.app.features.home.HomeCatalogDefinition
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.fanart.FanartService
import com.nuvio.app.features.fanart.FanartSettingsRepository
import com.nuvio.app.features.library.LibraryClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CachedCatalogPage(
    val timestamp: Long,
    val page: CatalogPage,
)

object AnilistCatalogRepository {
    private val log = Logger.withTag("AnilistCatalog")
    private val pageCache = mutableMapOf<String, CachedCatalogPage>()
    private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

    const val CATALOG_WATCHING = "anilist:watching"
    const val CATALOG_PLANNING = "anilist:planning"
    const val CATALOG_COMPLETED = "anilist:completed"
    const val CATALOG_REWATCHING = "anilist:rewatching"
    const val CATALOG_PAUSED = "anilist:paused"
    const val CATALOG_DROPPED = "anilist:dropped"
    const val CATALOG_TRENDING = "anilist:trending"
    const val CATALOG_POPULAR = "anilist:popular"
    const val CATALOG_TOP_RATED = "anilist:top-rated"
    const val CATALOG_AIRING = "anilist:airing"

    fun clearCache() {
        pageCache.clear()
    }

    fun getCatalogDefinitions(): List<HomeCatalogDefinition> {
        AnilistAuthRepository.ensureInitialized()
        val token = AnilistAuthRepository.token.value
        val isAuthenticated = !token.isNullOrBlank() || AnilistAuthRepository.isAuthenticated.value
        val list = mutableListOf<HomeCatalogDefinition>()

        if (isAuthenticated) {
            list.add(
                HomeCatalogDefinition(
                    key = "anilist:anime:watching",
                    defaultTitle = "Currently Watching",
                    catalogName = "Currently Watching",
                    addonName = "AniList",
                    manifestUrl = "native://anilist",
                    type = "anime",
                    catalogId = CATALOG_WATCHING,
                    supportsPagination = true,
                    descriptorSignature = "anilist:watching",
                )
            )
            list.add(
                HomeCatalogDefinition(
                    key = "anilist:anime:planning",
                    defaultTitle = "Plan to Watch",
                    catalogName = "Plan to Watch",
                    addonName = "AniList",
                    manifestUrl = "native://anilist",
                    type = "anime",
                    catalogId = CATALOG_PLANNING,
                    supportsPagination = true,
                    descriptorSignature = "anilist:planning",
                )
            )
        }

        list.add(
            HomeCatalogDefinition(
                key = "anilist:anime:trending",
                defaultTitle = "Trending Anime",
                catalogName = "Trending Anime",
                addonName = "AniList",
                manifestUrl = "native://anilist",
                type = "anime",
                catalogId = CATALOG_TRENDING,
                supportsPagination = true,
                descriptorSignature = "anilist:trending",
            )
        )
        list.add(
            HomeCatalogDefinition(
                key = "anilist:anime:airing",
                defaultTitle = "Currently Airing",
                catalogName = "Currently Airing",
                addonName = "AniList",
                manifestUrl = "native://anilist",
                type = "anime",
                catalogId = CATALOG_AIRING,
                supportsPagination = true,
                descriptorSignature = "anilist:airing",
            )
        )
        list.add(
            HomeCatalogDefinition(
                key = "anilist:anime:popular",
                defaultTitle = "Popular This Season",
                catalogName = "Popular This Season",
                addonName = "AniList",
                manifestUrl = "native://anilist",
                type = "anime",
                catalogId = CATALOG_POPULAR,
                supportsPagination = true,
                descriptorSignature = "anilist:popular",
            )
        )
        list.add(
            HomeCatalogDefinition(
                key = "anilist:anime:top-rated",
                defaultTitle = "Top Rated Anime",
                catalogName = "Top Rated Anime",
                addonName = "AniList",
                manifestUrl = "native://anilist",
                type = "anime",
                catalogId = CATALOG_TOP_RATED,
                supportsPagination = true,
                descriptorSignature = "anilist:top-rated",
            )
        )

        if (isAuthenticated) {
            list.add(
                HomeCatalogDefinition(
                    key = "anilist:anime:completed",
                    defaultTitle = "Completed",
                    catalogName = "Completed",
                    addonName = "AniList",
                    manifestUrl = "native://anilist",
                    type = "anime",
                    catalogId = CATALOG_COMPLETED,
                    supportsPagination = true,
                    descriptorSignature = "anilist:completed",
                )
            )
        }

        return list
    }

    suspend fun fetchCatalogPage(catalogId: String, page: Int = 1, perPage: Int = 25, force: Boolean = false): CatalogPage {
        AnilistAuthRepository.ensureInitialized()
        val token = AnilistAuthRepository.token.value
        val user = AnilistAuthRepository.currentUser.value ?: if (!token.isNullOrBlank()) {
            runCatching { AnilistApi.fetchCurrentUser(token) }.getOrNull()?.also { fetched ->
                AnilistAuthStorage.saveUser(fetched)
            }
        } else null

        val prefs = com.nuvio.app.features.anilist.AnilistPreferencesRepository.snapshot()
        val cacheKey = "$catalogId:$page:$perPage:${user?.name ?: "anon"}:${prefs.preferredTitleLanguage.name}"
        val now = LibraryClock.nowEpochMs()
        if (!force) {
            val cached = pageCache[cacheKey]
            if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
                return cached.page
            }
        }

        log.d { "fetchCatalogPage: catalogId=$catalogId, page=$page, tokenPresent=${!token.isNullOrBlank()}, force=$force" }

        val mediaList: List<AnilistMedia> = when (catalogId) {
            CATALOG_WATCHING -> {
                if (token.isNullOrBlank()) emptyList()
                else AnilistApi.fetchUserAnimeList(
                    token = token,
                    userName = user?.name,
                    status = AnilistMediaListStatus.CURRENT,
                    page = page,
                    perPage = perPage,
                )
            }
            CATALOG_PLANNING -> {
                if (token.isNullOrBlank()) emptyList()
                else AnilistApi.fetchUserAnimeList(
                    token = token,
                    userName = user?.name,
                    status = AnilistMediaListStatus.PLANNING,
                    page = page,
                    perPage = perPage,
                )
            }
            CATALOG_COMPLETED -> {
                if (token.isNullOrBlank()) emptyList()
                else AnilistApi.fetchUserAnimeList(
                    token = token,
                    userName = user?.name,
                    status = AnilistMediaListStatus.COMPLETED,
                    page = page,
                    perPage = perPage,
                )
            }
            CATALOG_REWATCHING -> {
                if (token.isNullOrBlank()) emptyList()
                else AnilistApi.fetchUserAnimeList(
                    token = token,
                    userName = user?.name,
                    status = AnilistMediaListStatus.REPEATING,
                    page = page,
                    perPage = perPage,
                )
            }
            CATALOG_PAUSED -> {
                if (token.isNullOrBlank()) emptyList()
                else AnilistApi.fetchUserAnimeList(
                    token = token,
                    userName = user?.name,
                    status = AnilistMediaListStatus.PAUSED,
                    page = page,
                    perPage = perPage,
                )
            }
            CATALOG_DROPPED -> {
                if (token.isNullOrBlank()) emptyList()
                else AnilistApi.fetchUserAnimeList(
                    token = token,
                    userName = user?.name,
                    status = AnilistMediaListStatus.DROPPED,
                    page = page,
                    perPage = perPage,
                )
            }
            CATALOG_TRENDING -> AnilistApi.fetchTrendingAnime(page = page, perPage = perPage)
            CATALOG_AIRING -> AnilistApi.fetchAiringAnime(page = page, perPage = perPage)
            CATALOG_POPULAR -> AnilistApi.fetchPopularSeasonAnime(page = page, perPage = perPage)
            CATALOG_TOP_RATED -> AnilistApi.fetchTopRatedAnime(page = page, perPage = perPage)
            else -> emptyList()
        }

        val previews = mediaList.map { media ->
            val itemId = "ani_${media.id}"
            val itemType = if (media.format == "MOVIE") "movie" else "series"

            MetaPreview(
                id = itemId,
                type = itemType,
                name = media.title?.getDisplayTitle(prefs.preferredTitleLanguage).orEmpty(),
                poster = FanartService.getCachedPoster(itemId, itemType)
                    ?: media.coverImage?.extraLarge
                    ?: media.coverImage?.large
                    ?: media.coverImage?.medium,
                banner = FanartService.getCachedBackdrop(itemId, itemType) ?: media.bannerImage,
                logo = FanartService.getCachedLogo(itemId, itemType),
                posterShape = PosterShape.Poster,
                description = media.description,
                releaseInfo = listOfNotNull(
                    com.nuvio.app.core.format.formatYearRange(media.startDateYear, media.endDateYear, media.status),
                    if (media.episodes != null && media.episodes > 0 && media.format != "MOVIE") {
                        "${media.episodes} eps"
                    } else {
                        media.duration?.let { "$it min" }
                    },
                ).joinToString(" • ").takeIf { it.isNotBlank() },
                imdbRating = if (media.averageScore != null && media.averageScore > 0) {
                    val score = (media.averageScore / 10.0)
                    "${(score * 10).toInt() / 10.0}"
                } else null,
                genres = media.genres,
            )
        }

        val result = CatalogPage(
            items = previews,
            rawItemCount = previews.size,
            nextSkip = if (mediaList.size >= perPage) (page * perPage) else null,
        )
        pageCache[cacheKey] = CachedCatalogPage(timestamp = now, page = result)

        val fanartSettings = FanartSettingsRepository.snapshot()
        if (fanartSettings.enabled && fanartSettings.hasApiKey && fanartSettings.usePosters) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                for (media in mediaList) {
                    val itemId = "ani_${media.id}"
                    val itemType = if (media.format == "MOVIE") "movie" else "series"
                    if (FanartService.getCachedPoster(itemId, itemType) == null) {
                        FanartService.resolvePoster(itemId, itemType)
                        kotlinx.coroutines.delay(80L)
                    }
                }
            }
        }

        return result
    }
}
