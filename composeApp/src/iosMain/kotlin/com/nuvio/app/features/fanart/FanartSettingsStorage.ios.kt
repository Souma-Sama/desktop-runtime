package com.nuvio.app.features.fanart

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object FanartSettingsStorage {
    private const val enabledKey = "fanart_enabled"
    private const val apiKey = "fanart_api_key"
    private const val useClearLogosKey = "fanart_use_clearlogos"
    private const val preferEnglishLogosKey = "fanart_prefer_english_logos"
    private const val useHeroBackdropsKey = "fanart_use_hero_backdrops"
    private const val usePostersKey = "fanart_use_posters"
    private const val useBannersKey = "fanart_use_banners"
    private const val useBetterPostersKey = "fanart_use_better_posters"
    private const val betterPostersTemplateKey = "fanart_better_posters_template"

    private const val qualityKey = "fanart_quality"
    private const val preferHdLogosKey = "fanart_prefer_hd_logos"
    private const val preferHdClearArtKey = "fanart_prefer_hd_clearart"

    private val syncKeys = listOf(
        enabledKey,
        apiKey,
        qualityKey,
        preferHdLogosKey,
        preferHdClearArtKey,
        useClearLogosKey,
        preferEnglishLogosKey,
        useHeroBackdropsKey,
        usePostersKey,
        useBannersKey,
        useBetterPostersKey,
        betterPostersTemplateKey,
    )

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)
    actual fun saveEnabled(enabled: Boolean) = saveBoolean(enabledKey, enabled)

    actual fun loadApiKey(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(apiKey))

    actual fun saveApiKey(apiKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(apiKey, forKey = ProfileScopedKey.of(this.apiKey))
    }

    actual fun loadQuality(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(qualityKey))

    actual fun saveQuality(qualityId: String) {
        NSUserDefaults.standardUserDefaults.setObject(qualityId, forKey = ProfileScopedKey.of(qualityKey))
    }

    actual fun loadPreferHdLogos(): Boolean? = loadBoolean(preferHdLogosKey)
    actual fun savePreferHdLogos(enabled: Boolean) = saveBoolean(preferHdLogosKey, enabled)

    actual fun loadPreferHdClearArt(): Boolean? = loadBoolean(preferHdClearArtKey)
    actual fun savePreferHdClearArt(enabled: Boolean) = saveBoolean(preferHdClearArtKey, enabled)

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

    actual fun loadBetterPostersTemplate(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(betterPostersTemplateKey))

    actual fun saveBetterPostersTemplate(template: String) {
        NSUserDefaults.standardUserDefaults.setObject(template, forKey = ProfileScopedKey.of(betterPostersTemplateKey))
    }

    private fun loadBoolean(key: String): Boolean? {
        val scopedKey = ProfileScopedKey.of(key)
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(scopedKey) != null) {
            defaults.boolForKey(scopedKey)
        } else {
            null
        }
    }

    private fun saveBoolean(key: String, value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = ProfileScopedKey.of(key))
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKey, encodeSyncString(it)) }
        loadQuality()?.let { put(qualityKey, encodeSyncString(it)) }
        loadPreferHdLogos()?.let { put(preferHdLogosKey, encodeSyncBoolean(it)) }
        loadPreferHdClearArt()?.let { put(preferHdClearArtKey, encodeSyncBoolean(it)) }
        loadUseClearLogos()?.let { put(useClearLogosKey, encodeSyncBoolean(it)) }
        loadPreferEnglishLogos()?.let { put(preferEnglishLogosKey, encodeSyncBoolean(it)) }
        loadUseHeroBackdrops()?.let { put(useHeroBackdropsKey, encodeSyncBoolean(it)) }
        loadUsePosters()?.let { put(usePostersKey, encodeSyncBoolean(it)) }
        loadUseBanners()?.let { put(useBannersKey, encodeSyncBoolean(it)) }
        loadUseBetterPosters()?.let { put(useBetterPostersKey, encodeSyncBoolean(it)) }
        loadBetterPostersTemplate()?.let { put(betterPostersTemplateKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        val defaults = NSUserDefaults.standardUserDefaults
        syncKeys.forEach { defaults.removeObjectForKey(ProfileScopedKey.of(it)) }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKey)?.let(::saveApiKey)
        payload.decodeSyncString(qualityKey)?.let(::saveQuality)
        payload.decodeSyncBoolean(preferHdLogosKey)?.let(::savePreferHdLogos)
        payload.decodeSyncBoolean(preferHdClearArtKey)?.let(::savePreferHdClearArt)
        payload.decodeSyncBoolean(useClearLogosKey)?.let(::saveUseClearLogos)
        payload.decodeSyncBoolean(preferEnglishLogosKey)?.let(::savePreferEnglishLogos)
        payload.decodeSyncBoolean(useHeroBackdropsKey)?.let(::saveUseHeroBackdrops)
        payload.decodeSyncBoolean(usePostersKey)?.let(::saveUsePosters)
        payload.decodeSyncBoolean(useBannersKey)?.let(::saveUseBanners)
        payload.decodeSyncBoolean(useBetterPostersKey)?.let(::saveUseBetterPosters)
        payload.decodeSyncString(betterPostersTemplateKey)?.let(::saveBetterPostersTemplate)
    }
}
