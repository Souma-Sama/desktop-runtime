package com.nuvio.app.features.anilist.streams

import com.nuvio.app.features.anilist.AnilistPreferencesRepository
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AnimeStreamIdManager {
    private val _availableOptions = MutableStateFlow<Map<Int, List<AnimeStreamIdOption>>>(emptyMap())
    val availableOptions: StateFlow<Map<Int, List<AnimeStreamIdOption>>> = _availableOptions.asStateFlow()

    fun registerOptions(
        anilistId: Int,
        imdbId: String?,
        kitsuId: String?,
        tmdbId: Int?,
        season: Int = 1,
    ): List<AnimeStreamIdOption> {
        val options = mutableListOf<AnimeStreamIdOption>()

        if (!imdbId.isNullOrBlank()) {
            options.add(
                AnimeStreamIdOption(
                    type = AnimeStreamIdType.IMDB,
                    rawId = imdbId,
                    formattedLabel = "IMDb ($imdbId)",
                    description = "Best for Torrentio, Real-Debrid, KnightCrawler",
                    isRecommended = true,
                    season = season,
                )
            )
        }

        if (!kitsuId.isNullOrBlank()) {
            val cleanKitsu = kitsuId.removePrefix("kitsu:")
            options.add(
                AnimeStreamIdOption(
                    type = AnimeStreamIdType.KITSU,
                    rawId = cleanKitsu,
                    formattedLabel = "Kitsu ($cleanKitsu)",
                    description = "Best for Anime Kitsu & CyberFlix",
                    isRecommended = imdbId.isNullOrBlank(),
                    season = season,
                )
            )
        }

        if (tmdbId != null && tmdbId > 0) {
            options.add(
                AnimeStreamIdOption(
                    type = AnimeStreamIdType.TMDB,
                    rawId = "$tmdbId",
                    formattedLabel = "TMDb ($tmdbId)",
                    description = "Best for TMDb & Cinemeta scrapers",
                    isRecommended = false,
                    season = season,
                )
            )
        }

        options.add(
            AnimeStreamIdOption(
                type = AnimeStreamIdType.ANILIST,
                rawId = "$anilistId",
                formattedLabel = "AniList ($anilistId)",
                description = "Native anime provider queries",
                isRecommended = false,
                season = season,
            )
        )

        _availableOptions.value = _availableOptions.value + (anilistId to options)
        return options
    }

    fun getOptions(anilistId: Int): List<AnimeStreamIdOption> {
        val base = _availableOptions.value[anilistId] ?: listOf(
            AnimeStreamIdOption(
                type = AnimeStreamIdType.ANILIST,
                rawId = "$anilistId",
                formattedLabel = "AniList ($anilistId)",
                description = "Native anime provider queries",
            )
        )
        val preferences = AnilistPreferencesRepository.snapshot()
        val overrideString = preferences.streamIdOverrides[anilistId]
        if (!overrideString.isNullOrBlank() && overrideString.startsWith("custom:", ignoreCase = true)) {
            val customVal = overrideString.removePrefix("custom:")
            val customOpt = AnimeStreamIdOption(
                type = AnimeStreamIdType.CUSTOM,
                rawId = customVal,
                formattedLabel = "Custom ($customVal)",
                description = "User specified ID override",
            )
            return base + customOpt
        }
        return base
    }

    fun getActiveOption(anilistId: Int): AnimeStreamIdOption {
        val options = getOptions(anilistId)
        val preferences = AnilistPreferencesRepository.snapshot()
        val overrideString = preferences.streamIdOverrides[anilistId]

        if (!overrideString.isNullOrBlank()) {
            if (overrideString.startsWith("custom:", ignoreCase = true)) {
                val customVal = overrideString.removePrefix("custom:")
                return AnimeStreamIdOption(
                    type = AnimeStreamIdType.CUSTOM,
                    rawId = customVal,
                    formattedLabel = "Custom ($customVal)",
                    description = "User specified ID override",
                )
            }
            val matched = options.firstOrNull { it.type.name.equals(overrideString, ignoreCase = true) }
            if (matched != null) return matched
        }

        return options.firstOrNull { it.isRecommended } ?: options.first()
    }

    fun selectOption(anilistId: Int, type: AnimeStreamIdType, customId: String? = null) {
        val overrideVal = when (type) {
            AnimeStreamIdType.CUSTOM -> if (!customId.isNullOrBlank()) "custom:${customId.trim()}" else null
            else -> type.name
        }
        AnilistPreferencesRepository.setStreamIdOverride(anilistId, overrideVal)
    }

    fun resolvePlaybackVideoId(
        parentMetaId: String?,
        season: Int,
        episode: Int,
        isMovie: Boolean,
        fallbackVideoId: String,
        relativeEpisode: Int = episode,
    ): String {
        val anilistId = AnilistTrackerCoordinator.extractAnilistId(parentMetaId)
            ?: return fallbackVideoId
        val activeOption = getActiveOption(anilistId)
        val targetSeason = if (activeOption.season == 0 || season == 0) 0 else if (activeOption.season > 0) activeOption.season else season
        return AnimeStreamIdFormatter.formatVideoId(
            option = activeOption,
            season = targetSeason,
            episode = episode,
            isMovie = isMovie,
            relativeEpisode = relativeEpisode,
        )
    }
}
