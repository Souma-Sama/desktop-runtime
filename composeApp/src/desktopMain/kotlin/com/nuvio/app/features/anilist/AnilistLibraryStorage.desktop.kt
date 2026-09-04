package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object AnilistLibraryStorage {
    private const val LIBRARY_KEY = "anilist_library_payload"
    private const val MENU_PREFS_KEY = "anilist_menu_prefs_payload"
    private val store = DesktopStorage.store("nuvio_anilist_library")

    actual fun loadLibraryPayload(): String? {
        return store.getString(ProfileScopedKey.of(LIBRARY_KEY))
    }

    actual fun saveLibraryPayload(payload: String) {
        store.putString(ProfileScopedKey.of(LIBRARY_KEY), payload)
    }

    actual fun loadMenuPrefsPayload(): String? {
        return store.getString(ProfileScopedKey.of(MENU_PREFS_KEY))
    }

    actual fun saveMenuPrefsPayload(payload: String) {
        store.putString(ProfileScopedKey.of(MENU_PREFS_KEY), payload)
    }
}
