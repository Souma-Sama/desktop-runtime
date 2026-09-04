package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AnilistLibraryMenuPrefs {
    private val log = Logger.withTag("AnilistMenuPrefs")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _state = MutableStateFlow(AnilistLibraryMenuPrefsState())
    val state: StateFlow<AnilistLibraryMenuPrefsState> = _state.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun setSortBy(sortBy: AnilistSortBy) {
        ensureLoaded()
        if (_state.value.sortBy == sortBy) return
        _state.value = _state.value.copy(sortBy = sortBy)
        persist()
    }

    fun setSortAscending(ascending: Boolean) {
        ensureLoaded()
        if (_state.value.sortAscending == ascending) return
        _state.value = _state.value.copy(sortAscending = ascending)
        persist()
    }

    fun setOpenByCatalogUrl(url: String?) {
        ensureLoaded()
        if (_state.value.openByCatalogUrl == url) return
        _state.value = _state.value.copy(openByCatalogUrl = url)
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = AnilistLibraryStorage.loadMenuPrefsPayload().orEmpty().trim()
        if (payload.isBlank()) {
            _state.value = AnilistLibraryMenuPrefsState()
            return
        }

        runCatching {
            _state.value = json.decodeFromString<AnilistLibraryMenuPrefsState>(payload)
        }.onFailure {
            log.w(it) { "Failed to parse Anilist menu prefs: ${it.message}" }
            _state.value = AnilistLibraryMenuPrefsState()
        }
    }

    private fun persist() {
        runCatching {
            val payload = json.encodeToString(_state.value)
            AnilistLibraryStorage.saveMenuPrefsPayload(payload)
        }.onFailure {
            log.w(it) { "Failed to persist Anilist menu prefs: ${it.message}" }
        }
    }
}
