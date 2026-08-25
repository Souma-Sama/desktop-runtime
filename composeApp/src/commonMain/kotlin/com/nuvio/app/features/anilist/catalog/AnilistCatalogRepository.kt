package com.nuvio.app.features.anilist.catalog

import co.touchlab.kermit.Logger
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistApi
import com.nuvio.app.features.anilist.AnilistMedia
import com.nuvio.app.features.anilist.AnilistMediaListStatus
import com.nuvio.app.features.catalog.CatalogPage
import com.nuvio.app.features.home.HomeCatalogDefinition
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape

object AnilistCatalogRepository {
    private val log = Logger.withTag("AnilistCatalog")

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

    fun getCatalogDefinitions(): List<HomeCatalogDefinition> {
        val isAuthenticated = AnilistAuthRepository.isAuthenticated.value
        val list = mutableListOf<HomeCatalogDefinition>()

        if (isAuthenticated) {
            list.add(
                HomeCatalogDefinition(
                    key = "anilist:anime:watching",
                    defaultTitle = "Currently Watching (AniList)",
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
                    defaultTitle = "Plan to Watch (AniList)",
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
                defaultTitle = "🔥 Trending Anime (AniList)",
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
                defaultTitle = "⚡ Currently Airing (AniList)",
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
                defaultTitle = "🌸 Popular This Season (AniList)",
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
                defaultTitle = "⭐ Top Rated Anime (AniList)",
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
                    defaultTitle = "Completed (AniList)",
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

    suspend fun fetchCatalogPage(catalogId: String, page: Int = 1, perPage: Int = 25): CatalogPage {
        val token = AnilistAuthRepository.token.value
        val user = AnilistAuthRepository.currentUser.value

        log.d { "fetchCatalogPage: catalogId=$catalogId, page=$page, tokenPresent=${!token.isNullOrBlank()}" }

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
            MetaPreview(
                id = "ani_${media.id}",
                type = if (media.format == "MOVIE") "movie" else "series",
                name = media.title?.displayTitle.orEmpty(),
                poster = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium,
                banner = media.bannerImage,
                posterShape = PosterShape.Poster,
                description = media.description,
                releaseInfo = if (media.episodes != null) "${media.episodes} eps" else null,
                imdbRating = if (media.averageScore != null && media.averageScore > 0) {
                    val score = (media.averageScore / 10.0)
                    "${(score * 10).toInt() / 10.0}"
                } else null,
                genres = media.genres,
            )
        }

        return CatalogPage(
            items = previews,
            rawItemCount = previews.size,
            nextSkip = if (mediaList.size >= perPage) (page * perPage) else null,
        )
    }
}
