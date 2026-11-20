package test

import domain.model.Board
import domain.model.Game
import domain.model.GameMode
import domain.model.GameStatus
import domain.service.GameTimeout
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameTimeoutTest {

    private val now = 1_000_000L
    private val playerX = UUID.randomUUID()
    private val playerO = UUID.randomUUID()

    private fun game(
        status: GameStatus = GameStatus.IN_PROGRESS,
        currentPlayer: UUID? = playerX,
        mode: GameMode = GameMode.HUMAN
    ) = Game(
        id = UUID.randomUUID(),
        board = Board(List(3) { List(3) { 0 } }),
        mode = mode,
        status = status,
        currentPlayer = currentPlayer,
        winner = null,
        playerX = playerX,
        playerO = playerO,
        createdAt = now - 100_000L
    )

    @Test
    fun playerWhoseTurnExpiredLoses() {
        val g = game(currentPlayer = playerX)

        val finished = GameTimeout.resolve(
            game = g,
            lastActivityX = now - 40_000L,
            lastActivityO = now - 40_000L,
            nowMs = now
        )

        assertEquals(GameStatus.FINISHED, finished?.status)
        assertEquals(playerO, finished?.winner)
        assertEquals(null, finished?.currentPlayer)
    }

    @Test
    fun opponentWinsForBotGamesToo() {
        val bot = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val g = game(currentPlayer = playerX, mode = GameMode.BOT).copy(playerO = bot)

        val finished = GameTimeout.resolve(
            game = g,
            lastActivityX = now - 41_000L,
            lastActivityO = 0L,
            nowMs = now
        )

        assertEquals(bot, finished?.winner)
    }

    @Test
    fun gameIsNotFinishedWhileTimeRemains() {
        val g = game()

        val finished = GameTimeout.resolve(
            game = g,
            lastActivityX = now - 39_000L,
            lastActivityO = 0L,
            nowMs = now
        )

        assertNull(finished)
    }

    @Test
    fun alreadyFinishedGameIsIgnored() {
        val g = game(status = GameStatus.FINISHED, currentPlayer = null)

        val finished = GameTimeout.resolve(
            game = g,
            lastActivityX = now - 100_000L,
            lastActivityO = 0L,
            nowMs = now
        )

        assertNull(finished)
    }
}
