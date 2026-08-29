package com.nuvio.app.features.anilist.catalog

object AnimeStudioLogos {
    private val studioLogos = mapOf<String, String>(
        // Verified CDN logos can be added here
    )

    fun findLogo(rawName: String?): String? {
        if (rawName.isNullOrBlank()) return null
        val normalized = rawName.trim().lowercase()
        studioLogos[normalized]?.let { return it }
        return studioLogos.entries.firstOrNull { (key, _) ->
            normalized.contains(key) || key.contains(normalized)
        }?.value
    }
}
