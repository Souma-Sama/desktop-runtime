package com.nuvio.app.features.anilist

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

internal actual object AnilistAuthStorage {
    private const val TOKEN_KEY = "nuvio_anilist_access_token"
    private const val USER_KEY = "nuvio_anilist_current_user"
    private val json = Json { ignoreUnknownKeys = true }
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun loadToken(): String? {
        return defaults.stringForKey(TOKEN_KEY)
    }

    actual fun saveToken(token: String?) {
        if (token.isNullOrBlank()) {
            defaults.removeObjectForKey(TOKEN_KEY)
        } else {
            defaults.setObject(token.trim(), forKey = TOKEN_KEY)
        }
    }

    actual fun loadUser(): AnilistUser? {
        val raw = defaults.stringForKey(USER_KEY) ?: return null
        return runCatching { json.decodeFromString<AnilistUser>(raw) }.getOrNull()
    }

    actual fun saveUser(user: AnilistUser?) {
        if (user == null) {
            defaults.removeObjectForKey(USER_KEY)
        } else {
            runCatching {
                defaults.setObject(json.encodeToString(user), forKey = USER_KEY)
            }
        }
    }

    actual fun clear() {
        defaults.removeObjectForKey(TOKEN_KEY)
        defaults.removeObjectForKey(USER_KEY)
    }
}
