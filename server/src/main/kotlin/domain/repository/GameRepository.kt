package domain.repository

import domain.model.FinishedGameItem
import domain.model.Game
import domain.model.GameActivity
import domain.model.GameStatus
import domain.model.LeaderboardEntry
import java.util.UUID

interface GameRepository {
    fun save(game: Game)

    fun get(id: UUID): Game?

    fun getAll(): List<Game>

    fun getWaitingGames(): List<Game>

    fun getActiveGamesForUser(userId: UUID): List<Game>

    fun getInProgressGames(): List<Game>

    fun getActivity(id: UUID): GameActivity?

    fun updateStatus(id: UUID, status: GameStatus)

    fun delete(id: UUID)

    fun updateActivityX(id: UUID, ts: Long)

    fun updateActivityO(id: UUID, ts: Long)

    fun getFinishedGamesHistory(userId: UUID): List<FinishedGameItem>

    fun getTopPlayers(limit: Int): List<LeaderboardEntry>
}
