package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.watchprogress.WatchProgressClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AnilistLibraryRepository {
    private val log = Logger.withTag("AnilistLibrary")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()

    private val _uiState = MutableStateFlow(AnilistLibraryUiState())
    val uiState: StateFlow<AnilistLibraryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var lastRefreshAtMs = 0L
    private const val CACHE_TTL_MS = 60_000L * 5 // 5 minutes cache TTL

    init {
        ensureLoaded()
        scope.launch {
            AnilistAuthRepository.currentUser.collect { user ->
                if (user != null) {
                    ensureFresh()
                }
            }
        }
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        loadSnapshotFromDisk()
    }

    fun onProfileChanged() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = AnilistLibraryUiState()
        ensureLoaded()
    }

    fun clearLocalState() {
        hasLoaded = false
        lastRefreshAtMs = 0L
        _uiState.value = AnilistLibraryUiState()
        runCatching { AnilistLibraryStorage.saveLibraryPayload("") }
    }

    private fun normalize(str: String): String =
        str.lowercase().filter { it.isLetterOrDigit() }

    fun allItems(): List<AnilistLibraryItem> {
        ensureLoaded()
        val state = _uiState.value
        return (state.watching + state.completed + state.planning +
                state.paused + state.dropped + state.rewatching)
    }

    fun isInLibrary(anilistId: Int): Boolean {
        return allItems().any { it.id == anilistId }
    }

    fun getMediaEntry(anilistId: Int): AnilistLibraryItem? {
        return allItems().firstOrNull { it.id == anilistId }
    }

    fun getMediaEntryByTitle(title: String): AnilistLibraryItem? {
        val clean = normalize(title)
        if (clean.isBlank()) return null
        return allItems().firstOrNull { normalize(it.title) == clean }
    }

    fun getMediaStatus(anilistId: Int): AnilistMediaListStatus? {
        val entry = getMediaEntry(anilistId) ?: return null
        return when (entry.status.uppercase()) {
            "CURRENT", "WATCHING" -> AnilistMediaListStatus.CURRENT
            "COMPLETED" -> AnilistMediaListStatus.COMPLETED
            "PLANNING", "PLAN_TO_WATCH" -> AnilistMediaListStatus.PLANNING
            "PAUSED", "ON_HOLD" -> AnilistMediaListStatus.PAUSED
            "DROPPED" -> AnilistMediaListStatus.DROPPED
            "REPEATING", "REWATCHING" -> AnilistMediaListStatus.REPEATING
            else -> {
                val state = _uiState.value
                when {
                    state.watching.any { it.id == anilistId } -> AnilistMediaListStatus.CURRENT
                    state.completed.any { it.id == anilistId } -> AnilistMediaListStatus.COMPLETED
                    state.planning.any { it.id == anilistId } -> AnilistMediaListStatus.PLANNING
                    state.paused.any { it.id == anilistId } -> AnilistMediaListStatus.PAUSED
                    state.dropped.any { it.id == anilistId } -> AnilistMediaListStatus.DROPPED
                    state.rewatching.any { it.id == anilistId } -> AnilistMediaListStatus.REPEATING
                    else -> null
                }
            }
        }
    }

    fun getMediaProgress(anilistId: Int): Pair<Int, Int?>? {
        val entry = getMediaEntry(anilistId) ?: return null
        return entry.progress to entry.totalEpisodes
    }

    fun getMediaStatusById(itemId: String, title: String? = null): AnilistMediaListStatus? {
        val anilistId = extractAnilistId(itemId)
        if (anilistId != null) {
            val st = getMediaStatus(anilistId)
            if (st != null) return st
        }
        if (!title.isNullOrBlank()) {
            val entry = getMediaEntryByTitle(title)
            if (entry != null) return getMediaStatus(entry.id)
        }
        return null
    }

    fun getMediaProgressById(itemId: String, title: String? = null): Pair<Int, Int?>? {
        val anilistId = extractAnilistId(itemId)
        if (anilistId != null) {
            val pr = getMediaProgress(anilistId)
            if (pr != null) return pr
        }
        if (!title.isNullOrBlank()) {
            val entry = getMediaEntryByTitle(title)
            if (entry != null) return getMediaProgress(entry.id)
        }
        return null
    }

    fun getUserScore(anilistId: Int): Double? {
        val entry = getMediaEntry(anilistId) ?: return null
        val raw = entry.score?.takeIf { it > 0.0 } ?: return null
        return if (raw >= 10.0) raw / 10.0 else raw
    }

    fun getUserScoreById(itemId: String, title: String? = null): Double? {
        val anilistId = extractAnilistId(itemId)
        if (anilistId != null) {
            val sc = getUserScore(anilistId)
            if (sc != null) return sc
        }
        if (!title.isNullOrBlank()) {
            val entry = getMediaEntryByTitle(title)
            if (entry != null) {
                val raw = entry.score?.takeIf { it > 0.0 }
                if (raw != null) return if (raw >= 10.0) raw / 10.0 else raw
            }
        }
        return null
    }

    fun updateItem(
        mediaId: Int,
        title: String? = null,
        status: AnilistMediaListStatus? = null,
        progress: Int? = null,
        score100: Double? = null,
        totalEpisodes: Int? = null,
        posterUrl: String? = null,
    ) {
        ensureLoaded()
        val all = allItems().toMutableList()
        val existingIndex = all.indexOfFirst { it.id == mediaId || (title != null && normalize(it.title) == normalize(title)) }
        val now = WatchProgressClock.nowEpochMs()

        val updatedItem = if (existingIndex >= 0) {
            val old = all[existingIndex]
            old.copy(
                status = status?.name ?: old.status,
                progress = progress ?: old.progress,
                score = score100 ?: old.score,
                totalEpisodes = totalEpisodes ?: old.totalEpisodes,
                posterUrl = posterUrl ?: old.posterUrl,
                updatedAt = now / 1000L,
            )
        } else if (status != null) {
            AnilistLibraryItem(
                id = mediaId,
                title = title ?: "Anime $mediaId",
                posterUrl = posterUrl,
                progress = progress ?: 0,
                totalEpisodes = totalEpisodes,
                score = score100,
                status = status.name,
                updatedAt = now / 1000L,
            )
        } else {
            null
        }

        if (updatedItem != null) {
            if (existingIndex >= 0) {
                all[existingIndex] = updatedItem
            } else {
                all.add(0, updatedItem)
            }
            applyUpdatedItems(all)
        }
    }

    fun removeItem(mediaId: Int) {
        ensureLoaded()
        val all = allItems().filterNot { it.id == mediaId }
        applyUpdatedItems(all)
    }

    private fun applyUpdatedItems(items: List<AnilistLibraryItem>) {
        val watching = items.filter { it.status.equals("CURRENT", ignoreCase = true) }
        val completed = items.filter { it.status.equals("COMPLETED", ignoreCase = true) }
        val planning = items.filter { it.status.equals("PLANNING", ignoreCase = true) }
        val paused = items.filter { it.status.equals("PAUSED", ignoreCase = true) }
        val dropped = items.filter { it.status.equals("DROPPED", ignoreCase = true) }
        val rewatching = items.filter { it.status.equals("REPEATING", ignoreCase = true) }

        _uiState.value = AnilistLibraryUiState(
            watching = watching,
            completed = completed,
            planning = planning,
            paused = paused,
            dropped = dropped,
            rewatching = rewatching,
            isLoading = false,
            isLoaded = true,
            errorMessage = null,
        )
        persistSnapshot(items)
    }

    fun extractAnilistId(itemId: String): Int? {
        return when {
            itemId.startsWith("ani_") -> itemId.removePrefix("ani_").toIntOrNull()
            itemId.startsWith("anilist:") -> itemId.removePrefix("anilist:").toIntOrNull()
            itemId.startsWith("anilist_") -> itemId.removePrefix("anilist_").toIntOrNull()
            itemId.toIntOrNull() != null -> itemId.toIntOrNull()
            else -> null
        }
    }

    suspend fun refreshNow() {
        refresh(force = true)
    }

    suspend fun ensureFresh() {
        refresh(force = false)
    }

    private suspend fun refresh(force: Boolean) {
        ensureLoaded()
        refreshMutex.withLock {
            val now = WatchProgressClock.nowEpochMs()
            if (!force && _uiState.value.isLoaded && now - lastRefreshAtMs <= CACHE_TTL_MS) {
                return
            }

            val token = AnilistAuthRepository.token.value
            val user = AnilistAuthRepository.currentUser.value

            if (token.isNullOrBlank() || user == null) {
                _uiState.value = AnilistLibraryUiState()
                lastRefreshAtMs = 0L
                return
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val items = AnilistApi.fetchMediaListCollection(user.id, token)

                val watching = items.filter { it.status.equals("CURRENT", ignoreCase = true) }
                val completed = items.filter { it.status.equals("COMPLETED", ignoreCase = true) }
                val planning = items.filter { it.status.equals("PLANNING", ignoreCase = true) }
                val paused = items.filter { it.status.equals("PAUSED", ignoreCase = true) }
                val dropped = items.filter { it.status.equals("DROPPED", ignoreCase = true) }
                val rewatching = items.filter { it.status.equals("REPEATING", ignoreCase = true) }

                val newState = AnilistLibraryUiState(
                    watching = watching,
                    completed = completed,
                    planning = planning,
                    paused = paused,
                    dropped = dropped,
                    rewatching = rewatching,
                    isLoading = false,
                    isLoaded = true,
                    errorMessage = null,
                )

                _uiState.value = newState
                lastRefreshAtMs = now
                persistSnapshot(items)
            } catch (e: Exception) {
                log.e(e) { "Failed to refresh AniList library: ${e.message}" }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to refresh library collections.",
                )
            }
        }
    }

    private fun loadSnapshotFromDisk() {
        hasLoaded = true
        val payload = AnilistLibraryStorage.loadLibraryPayload().orEmpty().trim()
        if (payload.isBlank()) {
            _uiState.value = AnilistLibraryUiState()
            return
        }

        runCatching {
            val items = json.decodeFromString<List<AnilistLibraryItem>>(payload)
            val watching = items.filter { it.status.equals("CURRENT", ignoreCase = true) }
            val completed = items.filter { it.status.equals("COMPLETED", ignoreCase = true) }
            val planning = items.filter { it.status.equals("PLANNING", ignoreCase = true) }
            val paused = items.filter { it.status.equals("PAUSED", ignoreCase = true) }
            val dropped = items.filter { it.status.equals("DROPPED", ignoreCase = true) }
            val rewatching = items.filter { it.status.equals("REPEATING", ignoreCase = true) }

            _uiState.value = AnilistLibraryUiState(
                watching = watching,
                completed = completed,
                planning = planning,
                paused = paused,
                dropped = dropped,
                rewatching = rewatching,
                isLoading = false,
                isLoaded = true,
                errorMessage = null,
            )
        }.onFailure {
            log.w(it) { "Failed to parse cached AniList library items: ${it.message}" }
            _uiState.value = AnilistLibraryUiState()
        }
    }

    private fun persistSnapshot(items: List<AnilistLibraryItem>) {
        runCatching {
            val payload = json.encodeToString(items)
            AnilistLibraryStorage.saveLibraryPayload(payload)
        }.onFailure {
            log.w(it) { "Failed to save cached AniList library: ${it.message}" }
        }
    }
}
