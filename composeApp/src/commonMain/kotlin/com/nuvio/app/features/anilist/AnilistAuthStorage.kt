package com.nuvio.app.features.anilist

internal expect object AnilistAuthStorage {
    fun loadToken(): String?
    fun saveToken(token: String?)
    fun loadUser(): AnilistUser?
    fun saveUser(user: AnilistUser?)
    fun clear()
}
