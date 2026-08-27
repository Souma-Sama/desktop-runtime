package com.nuvio.app.features.fanart

import kotlinx.serialization.Serializable

@Serializable
data class FanartSettings(
    val enabled: Boolean = true,
    val apiKey: String = "",
    val useClearLogos: Boolean = true,
    val preferEnglishLogos: Boolean = true,
    val useHeroBackdrops: Boolean = true,
    val usePosters: Boolean = true,
    val useBanners: Boolean = true,
    val useBetterPosters: Boolean = true,
    val betterPostersTemplate: String = "",
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
