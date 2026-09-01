package com.nuvio.app.features.anilist

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.artwork.MetaHubArtwork
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.components.AnimeStreamIdButton
import com.nuvio.app.features.details.components.AnimeTrackerButton
import com.nuvio.app.features.details.components.AnimeTrackerSheet
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.catalog.CatalogTarget

/**
 * KaiHooks: Central Sidecar Extension Bridge for Nuvio Kai.
 *
 * All Kai-specific anime features (AniList tracking, stream selection, AniChart,
 * score badges, and search enrichment) connect to upstream Nuvio through this
 * single hook interface. This ensures that future upstream Nuvio updates can be
 * merged cleanly with near-zero merge conflicts.
 */
object KaiHooks {
    const val MANIFEST_URL = "native://anilist"
    const val BUILTIN_URL = "builtin://anilist"
    const val ADDON_ID = "org.nuvio.anilist"

    /**
     * Determines whether a given media ID belongs to the native Nuvio-Kai ecosystem.
     */
    fun isKaiMedia(mediaId: String?): Boolean {
        if (mediaId.isNullOrBlank()) return false
        val id = mediaId.trim().lowercase()
        return id.startsWith("ani_") ||
               id.startsWith("ani:") ||
               id.startsWith("anilist:") ||
               id.startsWith("anilist_") ||
               id.startsWith("al:") ||
               id.startsWith("al_") ||
               id.startsWith("anichart:")
    }

    /**
     * Returns true if the AniList Kai extension is enabled in user preferences.
     */
    fun isKaiEnabled(): Boolean =
        AnilistPreferencesRepository.snapshot().enabled

    /**
     * Filters out native AniList addons from external addon lists when needed.
     */
    fun filterExternalAddons(addons: List<ManagedAddon>): List<ManagedAddon> {
        return addons.filterNot { isKaiAddon(it) }
    }

    /**
     * Checks if a ManagedAddon is the internal Nuvio-Kai AniList addon.
     */
    fun isKaiAddon(addon: ManagedAddon): Boolean {
        val url = addon.manifestUrl
        val id = addon.manifest?.id
        return url.startsWith(MANIFEST_URL) ||
               url.startsWith(BUILTIN_URL) ||
               id.equals(ADDON_ID, ignoreCase = true) ||
               id?.contains("anilist", ignoreCase = true) == true
    }

    /**
     * Checks if native AniList search should be included.
     */
    fun isKaiSearchEnabled(addons: List<ManagedAddon>): Boolean {
        val prefs = AnilistPreferencesRepository.snapshot()
        return prefs.enabled && (addons.isEmpty() || addons.any { isKaiAddon(it) && it.enabled })
    }

    /**
     * Executes native AniList anime search.
     */
    suspend fun executeSearch(query: String): HomeCatalogSection {
        val mediaList = AnilistApi.searchAnime(query)
        val previews = mediaList.map { media ->
            val score = if (media.averageScore != null && media.averageScore > 0) {
                media.averageScore / 10.0
            } else null
            MetaPreview(
                id = "ani_${media.id}",
                type = if (media.format == "MOVIE") "movie" else "series",
                name = media.title?.displayTitle.orEmpty(),
                poster = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium,
                banner = media.bannerImage,
                logo = MetaHubArtwork.getLogoUrl("ani_${media.id}"),
                posterShape = PosterShape.Poster,
                description = media.description,
                releaseInfo = listOfNotNull(
                    com.nuvio.app.core.format.formatYearRange(media.startDateYear, media.endDateYear, media.status),
                    if (media.episodes != null && media.episodes > 0 && media.format != "MOVIE") {
                        "${media.episodes} eps"
                    } else {
                        media.duration?.let { "$it min" }
                    },
                ).joinToString(" • ").takeIf { it.isNotBlank() },
                imdbRating = score?.let { "${((it * 10).toInt()) / 10.0}" },
                anilistScore = if (media.averageScore != null && media.averageScore > 0) media.averageScore.toDouble() else null,
                genres = media.genres,
            )
        }
        return HomeCatalogSection(
            key = "anilist:search:anime:${query.lowercase()}",
            title = "AniList Anime",
            subtitle = "AniList",
            addonName = "AniList",
            target = CatalogTarget.Anilist(
                catalogId = "anilist:search",
                contentType = "anime",
                supportsPagination = false,
            ),
            items = previews,
            availableItemCount = previews.size,
            hasMore = false,
        )
    }

    /**
     * Renders Details Page action buttons (AniList Tracker & Stream Provider)
     * strictly for Kai anime media.
     */
    @Composable
    fun DetailsActionButtons(
        meta: MetaDetails?,
        title: String?,
        iconButtonSize: Dp,
    ) {
        val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
        val isKaiItem = remember(meta?.id) { isKaiMedia(meta?.id) }

        if (anilistPrefs.enabled && isKaiItem) {
            Spacer(modifier = Modifier.width(12.dp))
            AnimeStreamIdButton(meta = meta, size = iconButtonSize)
            Spacer(modifier = Modifier.width(12.dp))
            AnimeTrackerButton(meta = meta, title = title, size = iconButtonSize)
        }
    }

    /**
     * Handles poster long-press/right-click: opens AnimeTrackerSheet for Kai media,
     * or invokes the default Nuvio quick-action overlay for standard media.
     */
    @Composable
    fun PosterAction(
        targetPreview: MetaPreview,
        onDismiss: () -> Unit,
        onDefaultContent: @Composable () -> Unit,
    ) {
        val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
        val isKai = remember(targetPreview.id) { isKaiMedia(targetPreview.id) }

        if (anilistPrefs.enabled && isKai) {
            AnimeTrackerSheet(
                preview = targetPreview,
                title = targetPreview.name,
                onDismiss = onDismiss,
            )
        } else {
            onDefaultContent()
        }
    }

    /**
     * Evaluates badges and scores for a poster card.
     */
    @Composable
    fun rememberPosterDecorations(item: MetaPreview): PosterDecorations {
        val anilistPrefs by AnilistPreferencesRepository.preferences.collectAsState()
        val isKaiItem = remember(item.id) { isKaiMedia(item.id) }

        val anilistScore = if (anilistPrefs.enabled && anilistPrefs.showPosterAnilistScore && isKaiItem) {
            item.anilistScore
        } else null

        val mediaStatus = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge && isKaiItem) {
            AnilistLibraryRepository.getMediaStatusById(item.id, item.name)
        } else null

        val mediaProgress = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge && isKaiItem) {
            AnilistLibraryRepository.getMediaProgressById(item.id, item.name)
        } else null

        val mediaUserScore = if (anilistPrefs.enabled && anilistPrefs.showPosterStatusBadge && isKaiItem) {
            AnilistLibraryRepository.getUserScoreById(item.id, item.name)
        } else null

        return remember(anilistScore, mediaStatus, mediaProgress, mediaUserScore, isKaiItem, anilistPrefs) {
            PosterDecorations(
                isKaiMedia = isKaiItem,
                anilistScore = anilistScore,
                libraryStatus = mediaStatus,
                libraryProgress = mediaProgress,
                userScore = mediaUserScore,
                showTitleLogos = anilistPrefs.enabled && anilistPrefs.showPosterTitleLogos && isKaiItem,
                showMalScore = anilistPrefs.enabled && anilistPrefs.showPosterMalScore && isKaiItem,
            )
        }
    }
}

data class PosterDecorations(
    val isKaiMedia: Boolean,
    val anilistScore: Double?,
    val libraryStatus: AnilistMediaListStatus?,
    val libraryProgress: Pair<Int, Int?>?,
    val userScore: Double?,
    val showTitleLogos: Boolean,
    val showMalScore: Boolean,
)
