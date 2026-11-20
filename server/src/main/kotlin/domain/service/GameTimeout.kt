package domain.service

import domain.model.Game
import domain.model.GameStatus

object GameTimeout {

    fun resolve(
        game: Game,
        lastActivityX: Long,
        lastActivityO: Long,
        nowMs: Long
    ): Game? {
        if (game.status != GameStatus.IN_PROGRESS) return null

        val timer = GameTimerCalculator.calculate(game, lastActivityX, lastActivityO, nowMs)
        if (timer.remaining > 0) return null

        val loser = game.currentPlayer ?: return null
        val winner = when (loser) {
            game.playerX -> game.playerO
            game.playerO -> game.playerX
            else -> null
        } ?: return null

        return game.copy(
            status = GameStatus.FINISHED,
            winner = winner,
            currentPlayer = null
        )
    }
}
