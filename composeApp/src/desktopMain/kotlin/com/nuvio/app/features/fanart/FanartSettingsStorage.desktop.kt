package com.nuvio.app.features.fanart

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object FanartSettingsStorage {
    private const val enabledKey = "fanart_enabled"
    private const val apiKey = "fanart_api_key"
    private const val useClearLogosKey = "fanart_use_clearlogos"
    private const val preferEnglishLogosKey = "fanart_prefer_english_logos"
    private const val useHeroBackdropsKey = "fanart_use_hero_backdrops"
    private const val usePostersKey = "fanart_use_posters"
    private const val useBannersKey = "fanart_use_banners"
    private const val useBetterPostersKey = "fanart_use_better_posters"
    private const val betterPostersTemplateKey = "fanart_better_posters_template"

    private val syncKeys = listOf(
        enabledKey,
        apiKey,
        useClearLogosKey,
        preferEnglishLogosKey,
        useHeroBackdropsKey,
        usePostersKey,
        useBannersKey,
        useBetterPostersKey,
        betterPostersTemplateKey,
    )
    private val store = DesktopStorage.store("nuvio_fanart_settings")

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)
    actual fun saveEnabled(enabled: Boolean) = saveBoolean(enabledKey, enabled)
    actual fun loadApiKey(): String? = loadString(apiKey)
    actual fun saveApiKey(apiKey: String) = saveString(this.apiKey, apiKey)
    actual fun loadUseClearLogos(): Boolean? = loadBoolean(useClearLogosKey)
    actual fun saveUseClearLogos(enabled: Boolean) = saveBoolean(useClearLogosKey, enabled)
    actual fun loadPreferEnglishLogos(): Boolean? = loadBoolean(preferEnglishLogosKey)
    actual fun savePreferEnglishLogos(enabled: Boolean) = saveBoolean(preferEnglishLogosKey, enabled)
    actual fun loadUseHeroBackdrops(): Boolean? = loadBoolean(useHeroBackdropsKey)
    actual fun saveUseHeroBackdrops(enabled: Boolean) = saveBoolean(useHeroBackdropsKey, enabled)
    actual fun loadUsePosters(): Boolean? = loadBoolean(usePostersKey)
    actual fun saveUsePosters(enabled: Boolean) = saveBoolean(usePostersKey, enabled)
    actual fun loadUseBanners(): Boolean? = loadBoolean(useBannersKey)
    actual fun saveUseBanners(enabled: Boolean) = saveBoolean(useBannersKey, enabled)
    actual fun loadUseBetterPosters(): Boolean? = loadBoolean(useBetterPostersKey)
    actual fun saveUseBetterPosters(enabled: Boolean) = saveBoolean(useBetterPostersKey, enabled)
    actual fun loadBetterPostersTemplate(): String? = loadString(betterPostersTemplateKey)
    actual fun saveBetterPostersTemplate(template: String) = saveString(betterPostersTemplateKey, template)

    private fun loadString(key: String): String? = store.getString(ProfileScopedKey.of(key))
    private fun saveString(key: String, value: String) = store.putString(ProfileScopedKey.of(key), value)
    private fun loadBoolean(key: String): Boolean? = store.getBoolean(ProfileScopedKey.of(key))
    private fun saveBoolean(key: String, value: Boolean) = store.putBoolean(ProfileScopedKey.of(key), value)

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKey, encodeSyncString(it)) }
        loadUseClearLogos()?.let { put(useClearLogosKey, encodeSyncBoolean(it)) }
        loadPreferEnglishLogos()?.let { put(preferEnglishLogosKey, encodeSyncBoolean(it)) }
        loadUseHeroBackdrops()?.let { put(useHeroBackdropsKey, encodeSyncBoolean(it)) }
        loadUsePosters()?.let { put(usePostersKey, encodeSyncBoolean(it)) }
        loadUseBanners()?.let { put(useBannersKey, encodeSyncBoolean(it)) }
        loadUseBetterPosters()?.let { put(useBetterPostersKey, encodeSyncBoolean(it)) }
        loadBetterPostersTemplate()?.let { put(betterPostersTemplateKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        store.removeAll(syncKeys.map(ProfileScopedKey::of))
        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKey)?.let(::saveApiKey)
        payload.decodeSyncBoolean(useClearLogosKey)?.let(::saveUseClearLogos)
        payload.decodeSyncBoolean(preferEnglishLogosKey)?.let(::savePreferEnglishLogos)
        payload.decodeSyncBoolean(useHeroBackdropsKey)?.let(::saveUseHeroBackdrops)
        payload.decodeSyncBoolean(usePostersKey)?.let(::saveUsePosters)
        payload.decodeSyncBoolean(useBannersKey)?.let(::saveUseBanners)
        payload.decodeSyncBoolean(useBetterPostersKey)?.let(::saveUseBetterPosters)
        payload.decodeSyncString(betterPostersTemplateKey)?.let(::saveBetterPostersTemplate)
    }
}
