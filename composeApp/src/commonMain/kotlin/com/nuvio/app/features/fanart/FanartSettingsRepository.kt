package com.nuvio.app.features.fanart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FanartSettingsRepository {
    private val _uiState = MutableStateFlow(FanartSettings())
    val uiState: StateFlow<FanartSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = false
    private var apiKey = ""
    private var useClearLogos = true
    private var preferEnglishLogos = true
    private var useHeroBackdrops = false
    private var usePosters = false
    private var useBanners = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): FanartSettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        FanartSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            FanartSettingsStorage.saveEnabled(false)
        }
        publish()
        FanartSettingsStorage.saveApiKey(normalized)
        FanartService.clearCache()
    }

    fun setUseClearLogos(value: Boolean) {
        ensureLoaded()
        if (useClearLogos == value) return
        useClearLogos = value
        publish()
        FanartSettingsStorage.saveUseClearLogos(value)
    }

    fun setPreferEnglishLogos(value: Boolean) {
        ensureLoaded()
        if (preferEnglishLogos == value) return
        preferEnglishLogos = value
        publish()
        FanartSettingsStorage.savePreferEnglishLogos(value)
        FanartService.clearCache()
    }

    fun setUseHeroBackdrops(value: Boolean) {
        ensureLoaded()
        if (useHeroBackdrops == value) return
        useHeroBackdrops = value
        publish()
        FanartSettingsStorage.saveUseHeroBackdrops(value)
    }

    fun setUsePosters(value: Boolean) {
        ensureLoaded()
        if (usePosters == value) return
        usePosters = value
        publish()
        FanartSettingsStorage.saveUsePosters(value)
    }

    fun setUseBanners(value: Boolean) {
        ensureLoaded()
        if (useBanners == value) return
        useBanners = value
        publish()
        FanartSettingsStorage.saveUseBanners(value)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = FanartSettingsStorage.loadApiKey().orEmpty().trim()
        enabled = (FanartSettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        useClearLogos = FanartSettingsStorage.loadUseClearLogos() ?: true
        preferEnglishLogos = FanartSettingsStorage.loadPreferEnglishLogos() ?: true
        useHeroBackdrops = FanartSettingsStorage.loadUseHeroBackdrops() ?: false
        usePosters = FanartSettingsStorage.loadUsePosters() ?: false
        useBanners = FanartSettingsStorage.loadUseBanners() ?: false
        publish()
    }

    private fun publish() {
        _uiState.value = FanartSettings(
            enabled = enabled,
            apiKey = apiKey,
            useClearLogos = useClearLogos,
            preferEnglishLogos = preferEnglishLogos,
            useHeroBackdrops = useHeroBackdrops,
            usePosters = usePosters,
            useBanners = useBanners,
        )
    }
}
