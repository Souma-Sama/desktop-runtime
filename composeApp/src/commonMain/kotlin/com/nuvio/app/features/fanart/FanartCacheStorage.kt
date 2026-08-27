package com.nuvio.app.features.fanart

internal expect object FanartCacheStorage {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun clear()
}
