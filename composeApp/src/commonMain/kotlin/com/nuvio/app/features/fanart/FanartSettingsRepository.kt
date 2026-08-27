package com.nuvio.app.features.fanart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FanartSettingsRepository {
    private val _uiState = MutableStateFlow(FanartSettings())
    val uiState: StateFlow<FanartSettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = true
    private var apiKey = ""
    private var useClearLogos = true
    private var preferEnglishLogos = true
    private var useHeroBackdrops = true
    private var usePosters = true
    private var useBanners = true
    private var useBetterPosters = true
    private var betterPostersTemplate = ""

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
        if (apiKey.isNotBlank()) {
            enabled = true
            FanartSettingsStorage.saveEnabled(true)
        } else {
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

    fun setUseBetterPosters(value: Boolean) {
        ensureLoaded()
        if (useBetterPosters == value) return
        useBetterPosters = value
        publish()
        FanartSettingsStorage.saveUseBetterPosters(value)
        FanartService.clearCache()
    }

    fun setBetterPostersTemplate(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (betterPostersTemplate == normalized) return
        betterPostersTemplate = normalized
        publish()
        FanartSettingsStorage.saveBetterPostersTemplate(normalized)
        FanartService.clearCache()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = FanartSettingsStorage.loadApiKey().orEmpty().trim()
        enabled = (FanartSettingsStorage.loadEnabled() ?: true) && apiKey.isNotBlank()
        useClearLogos = FanartSettingsStorage.loadUseClearLogos() ?: true
        preferEnglishLogos = FanartSettingsStorage.loadPreferEnglishLogos() ?: true
        useHeroBackdrops = FanartSettingsStorage.loadUseHeroBackdrops() ?: true
        usePosters = FanartSettingsStorage.loadUsePosters() ?: true
        useBanners = FanartSettingsStorage.loadUseBanners() ?: true
        useBetterPosters = FanartSettingsStorage.loadUseBetterPosters() ?: true
        betterPostersTemplate = FanartSettingsStorage.loadBetterPostersTemplate().orEmpty().trim()
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
            useBetterPosters = useBetterPosters,
            betterPostersTemplate = betterPostersTemplate,
        )
    }
}
