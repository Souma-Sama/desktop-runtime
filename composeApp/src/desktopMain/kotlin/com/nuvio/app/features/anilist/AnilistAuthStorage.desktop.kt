package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal actual object AnilistAuthStorage {
    private const val TOKEN_KEY = "anilist_access_token"
    private const val USER_KEY = "anilist_current_user"
    private val json = Json { ignoreUnknownKeys = true }
    private val store = DesktopStorage.store("nuvio_anilist_auth")

    actual fun loadToken(): String? {
        return store.getString(ProfileScopedKey.of(TOKEN_KEY))
    }

    actual fun saveToken(token: String?) {
        if (token.isNullOrBlank()) {
            store.remove(ProfileScopedKey.of(TOKEN_KEY))
        } else {
            store.putString(ProfileScopedKey.of(TOKEN_KEY), token.trim())
        }
    }

    actual fun loadUser(): AnilistUser? {
        val raw = store.getString(ProfileScopedKey.of(USER_KEY)) ?: return null
        return runCatching { json.decodeFromString<AnilistUser>(raw) }.getOrNull()
    }

    actual fun saveUser(user: AnilistUser?) {
        if (user == null) {
            store.remove(ProfileScopedKey.of(USER_KEY))
        } else {
            runCatching {
                store.putString(ProfileScopedKey.of(USER_KEY), json.encodeToString(user))
            }
        }
    }

    actual fun clear() {
        store.remove(ProfileScopedKey.of(TOKEN_KEY))
        store.remove(ProfileScopedKey.of(USER_KEY))
    }
}
