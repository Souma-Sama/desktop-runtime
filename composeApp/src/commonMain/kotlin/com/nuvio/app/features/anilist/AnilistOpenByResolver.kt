package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.ManagedAddon

object AnilistOpenByResolver {
    private val log = Logger.withTag("AnilistOpenBy")

    sealed interface Result {
        /** Open the detail screen with this [id] using the addon at [manifestUrl]. */
        data class OpenDirect(val id: String, val manifestUrl: String) : Result
        /** Fall back to title-based search. */
        data class SearchByTitle(val title: String) : Result
        /** Selected addon can't resolve this item. */
        data class NotSupported(val addonName: String) : Result
    }

    /**
     * Resolves how to open [item] using [addon].
     *
     * Resolution order:
     *  1. Addon declares `anilist:` or `ani_` id prefix → open directly as `anilist:{item.id}`
     *  2. Addon declares `tt` id prefix (IMDb) → resolve via ARM / ani.zip, then open as imdb id
     *  3. Otherwise → fallback to title search or NotSupported
     */
    suspend fun resolve(item: AnilistLibraryItem, addon: ManagedAddon): Result {
        val manifest = addon.manifest ?: return Result.NotSupported(addon.displayTitle)
        return when {
            supportsAniListId(manifest) -> {
                log.d { "OpenBy: anilist: prefix, opening anilist:${item.id}" }
                Result.OpenDirect(id = "anilist:${item.id}", manifestUrl = manifest.transportUrl)
            }
            supportsImdbId(manifest) -> resolveViaImdb(item, addon, manifest)
            else -> {
                log.w { "OpenBy: ${manifest.name} supports neither anilist: nor tt prefix" }
                Result.NotSupported(manifest.name)
            }
        }
    }

    /** Returns anime-capable addons (type == series, anime, or movie) from the current addon list. */
    fun animeAddons(): List<ManagedAddon> =
        AddonRepository.uiState.value.addons.filter { addon ->
            val manifest = addon.manifest
            addon.isActive && manifest != null &&
                manifest.types.any {
                    it.equals("series", ignoreCase = true) ||
                    it.equals("anime", ignoreCase = true) ||
                    it.equals("movie", ignoreCase = true)
                } &&
                manifest.resources.any { it.name.equals("meta", ignoreCase = true) }
        }

    private fun supportsAniListId(manifest: AddonManifest): Boolean =
        manifest.idPrefixes.any { it.startsWith("anilist", ignoreCase = true) || it.startsWith("ani_", ignoreCase = true) }

    private fun supportsImdbId(manifest: AddonManifest): Boolean =
        manifest.idPrefixes.any { it.startsWith("tt", ignoreCase = true) }

    private suspend fun resolveViaImdb(
        item: AnilistLibraryItem,
        addon: ManagedAddon,
        manifest: AddonManifest
    ): Result {
        val cached = item.imdbId
        if (!cached.isNullOrBlank()) {
            log.d { "OpenBy: using cached imdbId $cached for ${item.title}" }
            return Result.OpenDirect(id = cached, manifestUrl = manifest.transportUrl)
        }
        return try {
            val imdbId = AnilistResolutionService.resolveImdbId(item.id)
            if (!imdbId.isNullOrBlank()) {
                log.d { "OpenBy: resolved ${item.title} anilist:${item.id} -> $imdbId" }
                Result.OpenDirect(id = imdbId, manifestUrl = manifest.transportUrl)
            } else {
                log.w { "OpenBy: returned no imdbId for anilist:${item.id}" }
                Result.SearchByTitle(item.title)
            }
        } catch (e: Exception) {
            log.w(e) { "OpenBy: failed to resolve imdbId for anilist:${item.id}" }
            Result.SearchByTitle(item.title)
        }
    }
}
