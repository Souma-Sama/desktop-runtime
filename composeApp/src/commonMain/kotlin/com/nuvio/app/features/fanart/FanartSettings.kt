package com.nuvio.app.features.fanart

import kotlinx.serialization.Serializable

@Serializable
data class FanartSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val useClearLogos: Boolean = true,
    val preferEnglishLogos: Boolean = true,
    val useHeroBackdrops: Boolean = false,
    val usePosters: Boolean = false,
    val useBanners: Boolean = false,
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()
}
