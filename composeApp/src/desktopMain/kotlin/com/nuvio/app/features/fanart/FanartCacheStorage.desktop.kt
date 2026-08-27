package com.nuvio.app.features.fanart

import com.nuvio.app.core.storage.DesktopStorage

internal actual object FanartCacheStorage {
    private val store = DesktopStorage.store("nuvio_fanart_cache")

    actual fun get(key: String): String? = store.getString(key)
    actual fun put(key: String, value: String) = store.putString(key, value)
    actual fun clear() = store.clear()
}
