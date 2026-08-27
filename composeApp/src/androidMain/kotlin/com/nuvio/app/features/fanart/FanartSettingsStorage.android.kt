package com.nuvio.app.features.fanart

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.sync.decodeSyncBoolean
import com.nuvio.app.core.sync.decodeSyncString
import com.nuvio.app.core.sync.encodeSyncBoolean
import com.nuvio.app.core.sync.encodeSyncString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual object FanartSettingsStorage {
    private const val preferencesName = "nuvio_fanart_settings"
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

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? = loadBoolean(enabledKey)
    actual fun saveEnabled(enabled: Boolean) = saveBoolean(enabledKey, enabled)

    actual fun loadApiKey(): String? =
        preferences?.getString(ProfileScopedKey.of(apiKey), null)

    actual fun saveApiKey(apiKey: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(this.apiKey), apiKey)
            ?.apply()
    }

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
        preferences?.getString(ProfileScopedKey.of(betterPostersTemplateKey), null)

    actual fun saveBetterPostersTemplate(template: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(betterPostersTemplateKey), template)
            ?.apply()
    }

    private fun loadBoolean(key: String): Boolean? {
        val scopedKey = ProfileScopedKey.of(key)
        return if (preferences?.contains(scopedKey) == true) {
            preferences?.getBoolean(scopedKey, false)
        } else {
            null
        }
    }

    private fun saveBoolean(key: String, value: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(key), value)
            ?.apply()
    }

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
        preferences?.edit()?.apply {
            syncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

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
