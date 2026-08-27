package com.nuvio.app.features.fanart

import kotlinx.serialization.Serializable

@Serializable
enum class FanartArtworkQuality(val id: String, val label: String, val description: String) {
    HIGH("high", "Original (High Quality)", "Full resolution 1080p/4K backdrops and uncompressed posters"),
    MEDIUM("medium", "Optimized / Fast (Recommended)", "40x faster loading via compressed Fanart CDN preview cache"),
    LOW("low", "Data Saver (Low)", "Ultra-compact thumbnails for slow connections");

    companion object {
        fun fromId(id: String?): FanartArtworkQuality =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MEDIUM
    }
}

@Serializable
data class FanartSettings(
    val enabled: Boolean = true,
    val apiKey: String = "",
    val quality: FanartArtworkQuality = FanartArtworkQuality.MEDIUM,
    val preferHdLogos: Boolean = true,
    val preferHdClearArt: Boolean = true,
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
