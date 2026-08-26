package com.nuvio.app.features.fanart

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FanartImage(
    val id: String? = null,
    val url: String? = null,
    val lang: String? = null,
    val likes: String? = null,
    val season: String? = null,
)

@Serializable
data class FanartMovieResponse(
    val name: String? = null,
    @SerialName("tmdb_id") val tmdbId: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("hdmovielogo") val hdMovieLogo: List<FanartImage> = emptyList(),
    @SerialName("movielogo") val movieLogo: List<FanartImage> = emptyList(),
    @SerialName("moviebackground") val movieBackground: List<FanartImage> = emptyList(),
    @SerialName("movieposter") val moviePoster: List<FanartImage> = emptyList(),
    @SerialName("moviebanner") val movieBanner: List<FanartImage> = emptyList(),
    @SerialName("hdmovieclearart") val hdMovieClearArt: List<FanartImage> = emptyList(),
    @SerialName("movieart") val movieArt: List<FanartImage> = emptyList(),
)

@Serializable
data class FanartTvResponse(
    val name: String? = null,
    @SerialName("thetvdb_id") val tvdbId: String? = null,
    @SerialName("hdtvlogo") val hdTvLogo: List<FanartImage> = emptyList(),
    @SerialName("clearlogo") val clearLogo: List<FanartImage> = emptyList(),
    @SerialName("showbackground") val showBackground: List<FanartImage> = emptyList(),
    @SerialName("tvposter") val tvPoster: List<FanartImage> = emptyList(),
    @SerialName("tvbanner") val tvBanner: List<FanartImage> = emptyList(),
    @SerialName("seasonposter") val seasonPoster: List<FanartImage> = emptyList(),
    @SerialName("seasonbanner") val seasonBanner: List<FanartImage> = emptyList(),
    @SerialName("seasonthumb") val seasonThumb: List<FanartImage> = emptyList(),
    @SerialName("clearart") val clearArt: List<FanartImage> = emptyList(),
    @SerialName("tvart") val tvArt: List<FanartImage> = emptyList(),
)
