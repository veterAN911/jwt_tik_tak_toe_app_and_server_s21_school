package domain.service

import domain.model.Game
import domain.model.GameStatus
import domain.model.GameTimer

object GameTimerCalculator {

    const val WAITING_TOTAL_SECONDS = 60L
    const val MOVE_TOTAL_SECONDS = 40L

    fun calculate(
        game: Game,
        lastActivityX: Long,
        lastActivityO: Long,
        nowMs: Long
    ): GameTimer {
        val total = when (game.status) {
            GameStatus.WAITING -> WAITING_TOTAL_SECONDS
            else -> MOVE_TOTAL_SECONDS
        }

        val anchorMs = when (game.status) {
            GameStatus.WAITING -> game.createdAt
            GameStatus.IN_PROGRESS -> maxOf(
                lastActivityX.takeIf { it > 0 } ?: game.createdAt,
                lastActivityO.takeIf { it > 0 } ?: game.createdAt
            )
            else -> nowMs
        }

        val remainingMs = total * 1000L - (nowMs - anchorMs)

        val remaining = when (game.status) {
            GameStatus.WAITING, GameStatus.IN_PROGRESS ->
                ((remainingMs + 999L) / 1000L).coerceIn(0L, total)
            else -> 0L
        }

        return GameTimer(
            gameId = game.id,
            status = game.status,
            remaining = remaining,
            total = total
        )
    }
}
