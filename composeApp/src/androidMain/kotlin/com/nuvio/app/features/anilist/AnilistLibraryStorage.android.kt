package com.nuvio.app.features.anilist

import android.content.Context
import android.content.SharedPreferences

internal actual object AnilistLibraryStorage {
    private const val PREFS_NAME = "nuvio_anilist_library"
    private const val LIBRARY_KEY = "anilist_library_payload"
    private const val MENU_PREFS_KEY = "anilist_menu_prefs_payload"
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadLibraryPayload(): String? {
        return preferences?.getString(LIBRARY_KEY, null)
    }

    actual fun saveLibraryPayload(payload: String) {
        preferences?.edit()?.putString(LIBRARY_KEY, payload)?.apply()
    }

    actual fun loadMenuPrefsPayload(): String? {
        return preferences?.getString(MENU_PREFS_KEY, null)
    }

    actual fun saveMenuPrefsPayload(payload: String) {
        preferences?.edit()?.putString(MENU_PREFS_KEY, payload)?.apply()
    }
}
