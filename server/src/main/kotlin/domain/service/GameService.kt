package domain.service

import domain.model.*
import java.util.*

interface GameService {

    fun createNewGame(playerX: UUID): Game

    fun createNewHumanGame(playerX: UUID): Game

    fun joinGame(id: UUID, playerO: UUID): Game

    fun getGame(id: UUID): Game?

    fun saveGame(game: Game)

    fun getWaitingGames(): List<Game>

    fun getActiveGamesForUser(userId: UUID): List<Game>

    fun getGameTimer(id: UUID): GameTimer?

    fun validateMove(oldGame: Game, newBoard: Board)

    fun applyMove(game: Game, newBoard: Board): Game

    fun makeBotMove(game: Game): Game

    fun updateActivity(gameId: UUID, playerId: UUID)

    fun cancelWaitingGame(gameId: UUID, userId: UUID)

    fun getFinishedGamesHistory(userId: UUID): List<FinishedGameItem>

    fun getTopPlayers(limit: Int): List<LeaderboardEntry>

    fun finishTimedOutGames(nowMs: Long)
}
