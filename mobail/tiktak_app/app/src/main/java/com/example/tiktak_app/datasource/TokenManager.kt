package com.example.tiktak_app.datasource

import com.example.tiktak_app.datasource.database.dao.UserDao
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.network.api.AuthApi
import com.example.tiktak_app.datasource.network.dto.RefreshRequestDto
import com.example.tiktak_app.domain.model.AuthTokens
import com.example.tiktak_app.utils.JwtUtil
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val userDao: UserDao,
    private val authApi: AuthApi,
    private val jwtUtil: JwtUtil
) {

    private val refreshMutex = Mutex()

    suspend fun saveTokens(tokens: AuthTokens) {
        userDao.saveTokens(
            TokenEntity(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )
    }

    suspend fun getTokens(): AuthTokens? = userDao.getTokens()?.let {
        AuthTokens(accessToken = it.accessToken, refreshToken = it.refreshToken)
    }

    suspend fun clearTokens() = userDao.clearTokens()

    suspend fun getValidAccessToken(): String? = refreshMutex.withLock {
        val tokens = getTokens() ?: return@withLock null

        if (!jwtUtil.isExpired(tokens.accessToken)) {
            return@withLock tokens.accessToken
        }

        return@withLock refreshLocked(tokens.refreshToken)
    }

    suspend fun refreshAccessToken(): String? = refreshMutex.withLock {
        val refreshToken = getTokens()?.refreshToken ?: return@withLock null
        refreshLocked(refreshToken)
    }

    private suspend fun refreshLocked(refreshToken: String): String? {
        if (jwtUtil.isExpired(refreshToken)) {
            clearTokens()
            return null
        }

        return try {
            val response = authApi.refreshAccess(RefreshRequestDto(refreshToken))
            val body = response.body()

            if (!response.isSuccessful || body == null) {
                clearTokens()
                null
            } else {
                saveTokens(AuthTokens(body.accessToken, body.refreshToken))
                body.accessToken
            }
        } catch (e: Exception) {
            null
        }
    }
}
