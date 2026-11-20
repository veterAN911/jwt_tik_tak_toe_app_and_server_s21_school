package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.domain.model.User

interface AuthRepository {
    suspend fun login(login: String, password: String): Result<User>
    suspend fun register(login: String, password: String): Result<Unit>
    suspend fun autoLogin(): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun logout()
}
