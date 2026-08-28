package com.nuvio.app

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.nuvio.app.features.details.MetaDetailsScreen
import com.nuvio.app.features.details.PersonDetailScreen
import com.nuvio.app.features.details.TmdbEntityBrowseScreen
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.TmdbEntityKind
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.navigation.DetailRoute
import com.nuvio.app.navigation.EntityBrowseRoute
import com.nuvio.app.navigation.NuvioNavigator
import com.nuvio.app.navigation.PersonDetailRoute
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.person_role_creator
import nuvio.composeapp.generated.resources.person_role_director
import nuvio.composeapp.generated.resources.person_role_writer
import org.jetbrains.compose.resources.stringResource

internal typealias ContentPlayAction = (
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String?,
    poster: String?,
    background: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    episodeThumbnail: String?,
    pauseDescription: String?,
    resumePositionMs: Long?,
) -> Unit

@Composable
private fun rememberOpenMeta(navController: NuvioNavigator): (MetaPreview) -> Unit {
    val scope = rememberCoroutineScope()
    return { preview ->
        scope.launch {
            // 1. Direct AniList ID
            if (preview.id.startsWith("ani_", ignoreCase = true) || preview.id.startsWith("anilist:", ignoreCase = true)) {
                navController.navigate(
                    DetailRoute(
                        type = preview.type,
                        id = preview.id,
                        title = preview.name,
                    ),
                )
                return@launch
            }

            // 2. If it's an anime, resolve to standalone AniList season by searching title on AniList GraphQL
            val searchTitle = preview.name.trim()
            if (searchTitle.isNotBlank()) {
                val anilistMatches = runCatching {
                    com.nuvio.app.features.anilist.AnilistApi.searchAnime(query = searchTitle)
                }.getOrNull().orEmpty()

                if (anilistMatches.isNotEmpty()) {
                    val cleanQuery = searchTitle.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
                    val matched = anilistMatches.firstOrNull { m ->
                        val mEng = m.title?.english?.lowercase()?.replace(Regex("[^a-z0-9 ]"), " ")?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                        val mRom = m.title?.romaji?.lowercase()?.replace(Regex("[^a-z0-9 ]"), " ")?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                        val mDisp = m.title?.displayTitle?.lowercase()?.replace(Regex("[^a-z0-9 ]"), " ")?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
                        cleanQuery == mEng || cleanQuery == mRom || cleanQuery == mDisp ||
                            (cleanQuery.length >= 4 && (mEng.contains(cleanQuery) || cleanQuery.contains(mEng) || mRom.contains(cleanQuery) || cleanQuery.contains(mRom)))
                    } ?: (if (anilistMatches.size == 1) anilistMatches.first() else null)

                    if (matched != null) {
                        val mediaType = if (matched.format?.equals("MOVIE", ignoreCase = true) == true || matched.episodes == 1) "movie" else "series"
                        navController.navigate(
                            DetailRoute(
                                type = mediaType,
                                id = "ani_${matched.id}",
                                title = matched.title?.displayTitle ?: preview.name,
                            ),
                        )
                        return@launch
                    }
                }
            }

            // 3. Fallback for Western movies / series
            val resolvedId = if (preview.id.startsWith("tmdb:")) {
                val tmdbId = preview.id.removePrefix("tmdb:").toIntOrNull()
                tmdbId?.let {
                    TmdbService.tmdbToImdb(
                        tmdbId = it,
                        mediaType = preview.type,
                    )
                } ?: preview.id
            } else {
                preview.id
            }
            navController.navigate(
                DetailRoute(
                    type = preview.type,
                    id = resolvedId,
                    title = preview.name,
                ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DetailsDestination(
    route: DetailRoute,
    navController: NuvioNavigator,
    onPlay: ContentPlayAction,
    onPlayManually: ContentPlayAction,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    val onOpenMeta = rememberOpenMeta(navController)
    val directorRole = stringResource(Res.string.person_role_director)
    val writerRole = stringResource(Res.string.person_role_writer)
    val creatorRole = stringResource(Res.string.person_role_creator)
    MetaDetailsScreen(
        type = route.type,
        id = route.id,
        initialTitle = route.title,
        onBack = onBack,
        onPlay = onPlay,
        onPlayManually = onPlayManually,
        onOpenMeta = onOpenMeta,
        onCastClick = { person, avatarTransitionKey ->
            val personId = person.tmdbId ?: 0
            val isAnilistMedia = route.id.startsWith("ani_", ignoreCase = true) || route.id.startsWith("anilist:", ignoreCase = true)
            navController.navigate(
                PersonDetailRoute(
                    personId = personId,
                    personName = person.name,
                    personPhoto = person.photo,
                    castAvatarTransitionKey = avatarTransitionKey,
                    preferCrew = person.role?.let {
                        it.equals("Director", ignoreCase = true) ||
                            it.equals(directorRole, ignoreCase = true) ||
                            it.equals("Writer", ignoreCase = true) ||
                            it.equals(writerRole, ignoreCase = true) ||
                            it.equals("Creator", ignoreCase = true) ||
                            it.equals(creatorRole, ignoreCase = true)
                    } ?: false,
                    isAnilist = isAnilistMedia,
                ),
            )
        },
        onCompanyClick = { company, entityKind ->
            val entityId = company.tmdbId ?: 0
            val isAnilistMedia = route.id.startsWith("ani_", ignoreCase = true) || route.id.startsWith("anilist:", ignoreCase = true)
            navController.navigate(
                EntityBrowseRoute(
                    entityKind = entityKind,
                    entityId = entityId,
                    entityName = company.name,
                    sourceType = route.type,
                    isAnilist = isAnilistMedia,
                ),
            )
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PersonDestination(
    route: PersonDetailRoute,
    navController: NuvioNavigator,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    PersonDetailScreen(
        personId = route.personId,
        personName = route.personName,
        initialProfilePhoto = route.personPhoto,
        avatarTransitionKey = route.castAvatarTransitionKey,
        preferCrew = route.preferCrew,
        isAnilist = route.isAnilist,
        onBack = onBack,
        onOpenMeta = rememberOpenMeta(navController),
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun EntityDestination(
    route: EntityBrowseRoute,
    navController: NuvioNavigator,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    TmdbEntityBrowseScreen(
        entityKind = TmdbEntityKind.fromRouteValue(route.entityKind),
        entityId = route.entityId,
        entityName = route.entityName,
        sourceType = route.sourceType,
        isAnilist = route.isAnilist,
        onBack = onBack,
        onOpenMeta = rememberOpenMeta(navController),
        modifier = Modifier.fillMaxSize(),
    )
}
