package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.GameMode
import com.example.tiktak_app.domain.model.LeaderboardPlayer

interface GameRepository {

    suspend fun getWaitingGames(): Result<List<Game>>

    suspend fun createGame(mode: GameMode): Result<Game>

    suspend fun openGame(gameId: String, join: Boolean): Result<GameSession>

    suspend fun getFinishedGames(): Result<List<FinishedGame>>

    suspend fun getLeaderboard(): Result<List<LeaderboardPlayer>>
}
