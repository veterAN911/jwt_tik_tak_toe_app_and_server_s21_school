package com.example.tiktak_app.datasource.network.interceptor

import com.example.tiktak_app.datasource.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.url.encodedPath.startsWith("/auth/")) {
            return chain.proceed(request)
        }

        val token = runBlocking { tokenManager.getValidAccessToken() }
            ?: return chain.proceed(request)

        var response = chain.proceed(request.withToken(token))

        if (response.code == 401) {
            response.close()

            val newToken = runBlocking { tokenManager.refreshAccessToken() }
            response = if (newToken != null) {
                chain.proceed(request.withToken(newToken))
            } else {
                chain.proceed(request)
            }
        }

        return response
    }

    private fun Request.withToken(token: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
}
