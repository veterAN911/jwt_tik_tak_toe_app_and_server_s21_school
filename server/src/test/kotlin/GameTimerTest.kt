package test

import domain.model.Board
import domain.model.Game
import domain.model.GameMode
import domain.model.GameStatus
import domain.service.GameTimerCalculator
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GameTimerTest {

    private val now = 1_000_000L

    private fun game(
        status: GameStatus,
        createdAt: Long = now - 1_000L,
        mode: GameMode = GameMode.HUMAN
    ) = Game(
        id = UUID.randomUUID(),
        board = Board(List(3) { List(3) { 0 } }),
        mode = mode,
        status = status,
        currentPlayer = UUID.randomUUID(),
        winner = null,
        playerX = UUID.randomUUID(),
        playerO = UUID.randomUUID(),
        createdAt = createdAt
    )

    @Test
    fun waitingGameCountsSixtySecondsFromCreation() {
        val g = game(GameStatus.WAITING, createdAt = now - 30_000L)

        val timer = GameTimerCalculator.calculate(g, 0L, 0L, now)

        assertEquals(GameStatus.WAITING, timer.status)
        assertEquals(60L, timer.total)
        assertEquals(30L, timer.remaining)
    }

    @Test
    fun moveTimerCountsFortySecondsFromLastMove() {
        val g = game(GameStatus.IN_PROGRESS, createdAt = now - 100_000L)

        val timer = GameTimerCalculator.calculate(
            game = g,
            lastActivityX = now - 5_000L,
            lastActivityO = now - 50_000L,
            nowMs = now
        )

        assertEquals(40L, timer.total)
        assertEquals(35L, timer.remaining)
    }

    @Test
    fun activitiesAreIgnoredIfNeverSet() {
        val g = game(GameStatus.IN_PROGRESS, createdAt = now - 5_000L)

        val timer = GameTimerCalculator.calculate(g, 0L, 0L, now)

        assertEquals(35L, timer.remaining)
    }

    @Test
    fun moveTimerEndsAtExactlyFortySeconds() {
        val g = game(GameStatus.IN_PROGRESS, createdAt = now - 100_000L)

        val timer = GameTimerCalculator.calculate(
            game = g,
            lastActivityX = now - 40_000L,
            lastActivityO = 0L,
            nowMs = now
        )

        assertEquals(40L, timer.total)
        assertEquals(0L, timer.remaining)
    }

    @Test
    fun remainingNeverGoesBelowZero() {
        val g = game(GameStatus.IN_PROGRESS, createdAt = now - 100_000L)

        val timer = GameTimerCalculator.calculate(
            game = g,
            lastActivityX = now - 60_000L,
            lastActivityO = 0L,
            nowMs = now
        )

        assertEquals(0L, timer.remaining)
    }

    @Test
    fun finishedGameHasZeroRemaining() {
        val g = game(GameStatus.FINISHED)

        val timer = GameTimerCalculator.calculate(g, now, now, now)

        assertEquals(0L, timer.remaining)
    }
}
