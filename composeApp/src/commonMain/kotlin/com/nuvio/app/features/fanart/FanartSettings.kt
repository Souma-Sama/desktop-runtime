package com.nuvio.app.features.fanart

import kotlinx.serialization.Serializable

@Serializable
data class FanartSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val useClearLogos: Boolean = true,
    val preferEnglishLogos: Boolean = true,
    val useHeroBackdrops: Boolean = false,
    val usePosters: Boolean = true,
    val useBanners: Boolean = false,
    val useBetterPosters: Boolean = true,
    val betterPostersTemplate: String = "",
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
