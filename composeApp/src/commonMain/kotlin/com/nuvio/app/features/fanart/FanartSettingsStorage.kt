package com.nuvio.app.features.fanart

import kotlinx.serialization.json.JsonObject

internal expect object FanartSettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun loadApiKey(): String?
    fun saveApiKey(apiKey: String)
    fun loadUseClearLogos(): Boolean?
    fun saveUseClearLogos(enabled: Boolean)
    fun loadPreferEnglishLogos(): Boolean?
    fun savePreferEnglishLogos(enabled: Boolean)
    fun loadUseHeroBackdrops(): Boolean?
    fun saveUseHeroBackdrops(enabled: Boolean)
    fun loadUsePosters(): Boolean?
    fun saveUsePosters(enabled: Boolean)
    fun loadUseBanners(): Boolean?
    fun saveUseBanners(enabled: Boolean)
    fun loadUseBetterPosters(): Boolean?
    fun saveUseBetterPosters(enabled: Boolean)
    fun loadBetterPostersTemplate(): String?
    fun saveBetterPostersTemplate(template: String)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
