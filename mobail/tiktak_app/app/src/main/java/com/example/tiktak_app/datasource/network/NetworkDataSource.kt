package com.example.tiktak_app.datasource.network

import com.example.tiktak_app.datasource.network.api.ApiService
import com.example.tiktak_app.datasource.network.api.AuthApi
import com.example.tiktak_app.datasource.network.dto.FinishedGameDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.LeaderboardDto
import com.example.tiktak_app.datasource.network.dto.LoginRequestDto
import com.example.tiktak_app.datasource.network.dto.TokenResponseDto
import com.example.tiktak_app.datasource.network.dto.UserDto
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDataSource @Inject constructor(
    private val authApi: AuthApi,
    private val apiService: ApiService
) {

    suspend fun login(login: String, password: String): Response<TokenResponseDto> =
        authApi.login(LoginRequestDto(login, password))

    suspend fun signUp(login: String, password: String): Response<Unit> =
        authApi.signUp(LoginRequestDto(login, password))

    suspend fun getUser(userId: String): Response<UserDto> =
        apiService.getUser(userId)

    suspend fun getFinishedGames(): Response<List<FinishedGameDto>> =
        apiService.getFinishedGames()

    suspend fun getLeaderboard(limit: Int): Response<List<LeaderboardDto>> =
        apiService.getLeaderboard(limit)

    suspend fun getWaitingGames(): Response<List<GameDto>> =
        apiService.getWaitingGames()
}
