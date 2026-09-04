package com.nuvio.app.features.anilist

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal actual object AnilistAuthStorage {
    private const val PREFS_NAME = "nuvio_anilist_auth"
    private const val TOKEN_KEY = "anilist_access_token"
    private const val USER_KEY = "anilist_current_user"
    private val json = Json { ignoreUnknownKeys = true }
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadToken(): String? {
        return preferences?.getString(TOKEN_KEY, null)
    }

    actual fun saveToken(token: String?) {
        preferences?.edit()?.apply {
            if (token.isNullOrBlank()) {
                remove(TOKEN_KEY)
            } else {
                putString(TOKEN_KEY, token.trim())
            }
            apply()
        }
    }

    actual fun loadUser(): AnilistUser? {
        val raw = preferences?.getString(USER_KEY, null) ?: return null
        return runCatching { json.decodeFromString<AnilistUser>(raw) }.getOrNull()
    }

    actual fun saveUser(user: AnilistUser?) {
        preferences?.edit()?.apply {
            if (user == null) {
                remove(USER_KEY)
            } else {
                runCatching {
                    putString(USER_KEY, json.encodeToString(user))
                }
            }
            apply()
        }
    }

    actual fun clear() {
        preferences?.edit()?.clear()?.apply()
    }
}
