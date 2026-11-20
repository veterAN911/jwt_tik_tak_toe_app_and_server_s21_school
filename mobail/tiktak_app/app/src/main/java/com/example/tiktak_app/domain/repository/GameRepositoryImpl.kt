package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.datasource.database.DatabaseDataSource
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.mapper.toDomain
import com.example.tiktak_app.datasource.mapper.toEntity
import com.example.tiktak_app.datasource.network.NetworkDataSource
import com.example.tiktak_app.datasource.network.apiErrorMessage
import com.example.tiktak_app.datasource.network.socket.GameSocket
import com.example.tiktak_app.datasource.network.socket.GameSocketClient
import com.example.tiktak_app.datasource.network.socket.SocketEvent
import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.GameMode
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val network: NetworkDataSource,
    private val database: DatabaseDataSource,
    private val socketClient: GameSocketClient
) : GameRepository {

    override suspend fun getWaitingGames(): Result<List<Game>> {
        return try {
            val response = network.getWaitingGames()

            if (response.isSuccessful) {
                Result.success(response.body().orEmpty().map { it.toDomain() })
            } else {
                Result.failure(
                    ApiException(
                        response.code(),
                        response.apiErrorMessage("Не удалось загрузить игры")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGame(mode: GameMode): Result<Game> =
        socketClient.createGame(mode.name)
            .map { it.toDomain() }

    override suspend fun openGame(gameId: String, join: Boolean): Result<GameSession> =
        socketClient.watchGame(gameId, join)
            .map { SocketGameSession(it) }

    override suspend fun getFinishedGames(): Result<List<FinishedGame>> {
        return try {
            val response = network.getFinishedGames()

            if (!response.isSuccessful) {
                return Result.failure(
                    ApiException(
                        response.code(),
                        response.apiErrorMessage("Не удалось загрузить историю игр")
                    )
                )
            }

            val games = response.body()
                .orEmpty()
                .map { it.toDomain() }
                .sortedByDescending { it.createdAt }

            database.replaceHistory(games.map { it.toEntity() })

            Result.success(database.getHistory().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLeaderboard(): Result<List<LeaderboardPlayer>> {
        return try {
            val response = network.getLeaderboard(LEADERBOARD_LIMIT)

            if (!response.isSuccessful) {
                return Result.failure(
                    ApiException(
                        response.code(),
                        response.apiErrorMessage("Не удалось загрузить таблицу лидеров")
                    )
                )
            }

            val players = response.body()
                .orEmpty()
                .map { it.toDomain() }
                .sortedByDescending { it.winsRatio }
                .take(LEADERBOARD_LIMIT)

            val entities = players.mapIndexed { index, player ->
                LeaderboardEntity(
                    userId = player.userId,
                    login = player.login,
                    ratio = player.winsRatio,
                    position = index
                )
            }
            database.replaceLeaderboard(entities)

            Result.success(database.getLeaderboard().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val LEADERBOARD_LIMIT = 20
    }
}

private class SocketGameSession(
    private val socket: GameSocket
) : GameSession {

    override val events: Flow<GameEvent> = socket.events.map { event ->
        when (event) {
            is SocketEvent.Game -> GameEvent.GameUpdated(event.game.toDomain())
            is SocketEvent.Error -> GameEvent.Error(event.message)
            is SocketEvent.Closed -> GameEvent.Error(
                event.reason?.takeIf { it.isNotBlank() }
                    ?: "Соединение с сервером закрыто"
            )
        }
    }

    override fun makeMove(board: List<List<Int>>) = socket.makeMove(board)

    override fun close() = socket.close()
}
