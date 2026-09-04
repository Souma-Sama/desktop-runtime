package com.nuvio.app.features.anilist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AnilistAuthRepository {
    const val CLIENT_ID = "41382"
    const val OAUTH_AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize?client_id=$CLIENT_ID&response_type=token"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _currentUser = MutableStateFlow<AnilistUser?>(null)
    val currentUser: StateFlow<AnilistUser?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true

        val savedToken = AnilistAuthStorage.loadToken()
        val savedUser = AnilistAuthStorage.loadUser()

        if (!savedToken.isNullOrBlank()) {
            _token.value = savedToken
            _currentUser.value = savedUser
            _isAuthenticated.value = true

            // Refresh user profile asynchronously
            scope.launch {
                refreshUser()
            }
        }
    }

    suspend fun loginWithToken(token: String): Boolean {
        val cleanToken = token.trim().removePrefix("Bearer ").trim()
        if (cleanToken.isBlank()) return false

        _isAuthenticating.value = true
        val user = runCatching { AnilistApi.fetchCurrentUser(cleanToken) }.getOrNull()
        _isAuthenticating.value = false

        if (user != null) {
            _token.value = cleanToken
            _currentUser.value = user
            _isAuthenticated.value = true

            AnilistAuthStorage.saveToken(cleanToken)
            AnilistAuthStorage.saveUser(user)
            com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
            return true
        }

        return false
    }

    suspend fun refreshUser(): Boolean {
        val activeToken = _token.value ?: return false
        val user = runCatching { AnilistApi.fetchCurrentUser(activeToken) }.getOrNull() ?: return false
        _currentUser.value = user
        AnilistAuthStorage.saveUser(user)
        return true
    }

    fun handleAuthCallback(url: String): Boolean {
        if (!url.contains("access_token=") && !url.contains("anilist")) return false
        val token = extractTokenFromUrl(url) ?: return false
        scope.launch {
            loginWithToken(token)
        }
        return true
    }

    private fun extractTokenFromUrl(url: String): String? {
        val regex = Regex("""access_token=([^&#]+)""")
        val match = regex.find(url) ?: return null
        return match.groupValues.getOrNull(1)
    }

    fun logout() {
        _token.value = null
        _currentUser.value = null
        _isAuthenticated.value = false
        AnilistAuthStorage.clear()
        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.clearCache()
    }
}
