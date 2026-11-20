package com.example.tiktak_app.datasource.network.api

import com.example.tiktak_app.datasource.network.dto.FinishedGameDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.LeaderboardDto
import com.example.tiktak_app.datasource.network.dto.UserDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("/user/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserDto>

    @GET("/user/games/finished")
    suspend fun getFinishedGames(): Response<List<FinishedGameDto>>

    @GET("/leaderboard")
    suspend fun getLeaderboard(@Query("n") limit: Int): Response<List<LeaderboardDto>>

    @GET("/game/waiting")
    suspend fun getWaitingGames(): Response<List<GameDto>>
}
