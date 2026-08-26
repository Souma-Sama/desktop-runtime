package com.nuvio.app.features.anilist

internal expect object AnilistLibraryStorage {
    fun loadLibraryPayload(): String?
    fun saveLibraryPayload(payload: String)
    fun loadMenuPrefsPayload(): String?
    fun saveMenuPrefsPayload(payload: String)
}
