package com.nuvio.app.features.fanart

import platform.Foundation.NSUserDefaults

internal actual object FanartCacheStorage {
    actual fun get(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun put(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }

    actual fun clear() {
        val defs = NSUserDefaults.standardUserDefaults
        val dict = defs.dictionaryRepresentation()
        for (k in dict.keys) {
            val strKey = k as? String ?: continue
            if (strKey.startsWith("poster:") || strKey.startsWith("backdrop:") || strKey.startsWith("logo:") || strKey.startsWith("season:") || strKey.startsWith("lookup:") || strKey.startsWith("arm:")) {
                defs.removeObjectForKey(strKey)
            }
        }
    }
}
