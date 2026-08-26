package com.nuvio.app.features.anilist

import platform.Foundation.NSUserDefaults

internal actual object AnilistLibraryStorage {
    private const val LIBRARY_KEY = "nuvio_anilist_library_payload"
    private const val MENU_PREFS_KEY = "nuvio_anilist_menu_prefs_payload"
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun loadLibraryPayload(): String? {
        return defaults.stringForKey(LIBRARY_KEY)
    }

    actual fun saveLibraryPayload(payload: String) {
        defaults.setObject(payload, forKey = LIBRARY_KEY)
    }

    actual fun loadMenuPrefsPayload(): String? {
        return defaults.stringForKey(MENU_PREFS_KEY)
    }

    actual fun saveMenuPrefsPayload(payload: String) {
        defaults.setObject(payload, forKey = MENU_PREFS_KEY)
    }
}
