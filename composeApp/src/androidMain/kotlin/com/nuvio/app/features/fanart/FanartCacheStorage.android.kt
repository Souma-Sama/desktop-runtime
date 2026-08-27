package com.nuvio.app.features.fanart

import android.content.Context
import android.content.SharedPreferences

internal actual object FanartCacheStorage {
    private const val PREFS_NAME = "nuvio_fanart_cache"
    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun get(key: String): String? = preferences?.getString(key, null)

    actual fun put(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    actual fun clear() {
        preferences?.edit()?.clear()?.apply()
    }
}
