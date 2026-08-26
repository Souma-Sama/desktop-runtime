package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object AnilistResolutionService {
    private val log = Logger.withTag("AnilistResolution")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val ANIZIP_API = "https://api.ani.zip/mappings"
    private const val ARM_API = "https://arm.haglund.dev/api/v2"

    /**
     * Resolves the IMDb ID for a given AniList ID.
     * Uses the ARM API mapping first, then falls back to ani.zip.
     * Returns null if not found or on network/parse failure.
     */
    suspend fun resolveImdbId(anilistId: Int): String? {
        // 1. Try ARM API mapping
        try {
            val url = "$ARM_API/ids?source=anilist&id=$anilistId"
            val text = httpGetText(url)
            val jsonElement = json.parseToJsonElement(text) as? JsonObject
            val imdbId = jsonElement?.get("imdb")?.jsonPrimitive?.contentOrNull
            if (!imdbId.isNullOrBlank()) {
                return imdbId
            }
        } catch (e: Exception) {
            log.w(e) { "resolveImdbId: ARM API failed for anilist:$anilistId" }
        }

        // 2. Fallback to ani.zip
        val url = "$ANIZIP_API?anilist_id=$anilistId"
        return try {
            val text = httpGetText(url)
            val parsed = json.decodeFromString<AniZipResponse>(text)
            parsed.mappings?.imdbId?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            log.w(e) { "resolveImdbId: ani.zip failed for anilist:$anilistId" }
            null
        }
    }
}
