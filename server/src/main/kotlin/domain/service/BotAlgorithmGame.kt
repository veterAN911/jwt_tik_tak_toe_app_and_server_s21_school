package domain.service

import domain.model.BotDifficultyLevel
import kotlin.random.Random

object BotAlgorithmGame {

    private const val WIN_SCORE = 10

    fun chooseMove(
        board: List<List<Int>>,
        bot: Int,
        human: Int,
        difficulty: Double = BotDifficultyLevel.MAX,
        random: Random = Random.Default
    ): Pair<Int, Int>? {
        val free = freeCells(board)
        if (free.isEmpty()) return null

        val perfectPlay = random.nextDouble() < BotDifficultyPolicy.clamp(difficulty)
        return if (perfectPlay) bestMove(board, bot, human, free) else free.random(random)
    }

    private fun bestMove(
        board: List<List<Int>>,
        bot: Int,
        human: Int,
        free: List<Pair<Int, Int>>
    ): Pair<Int, Int> {
        var bestScore = Int.MIN_VALUE
        var bestCell = free.first()

        for ((x, y) in free) {
            val next = board.map { it.toMutableList() }
            next[x][y] = bot
            val score = minimax(next, bot, human, depth = 1, botTurn = false)
            if (score > bestScore) {
                bestScore = score
                bestCell = x to y
            }
        }

        return bestCell
    }

    private fun minimax(
        board: List<List<Int>>,
        bot: Int,
        human: Int,
        depth: Int,
        botTurn: Boolean
    ): Int {
        winnerOf(board)?.let { winner ->

            return if (winner == bot) WIN_SCORE - depth else depth - WIN_SCORE
        }

        val free = freeCells(board)
        if (free.isEmpty()) return 0

        var best = if (botTurn) Int.MIN_VALUE else Int.MAX_VALUE
        for ((x, y) in free) {
            val next = board.map { it.toMutableList() }
            next[x][y] = if (botTurn) bot else human
            val score = minimax(next, bot, human, depth + 1, !botTurn)
            best = if (botTurn) maxOf(best, score) else minOf(best, score)
        }
        return best
    }

    private fun winnerOf(board: List<List<Int>>): Int? {
        val lines = listOf(
            listOf(0 to 0, 0 to 1, 0 to 2),
            listOf(1 to 0, 1 to 1, 1 to 2),
            listOf(2 to 0, 2 to 1, 2 to 2),
            listOf(0 to 0, 1 to 0, 2 to 0),
            listOf(0 to 1, 1 to 1, 2 to 1),
            listOf(0 to 2, 1 to 2, 2 to 2),
            listOf(0 to 0, 1 to 1, 2 to 2),
            listOf(0 to 2, 1 to 1, 2 to 0)
        )

        for ((a, b, c) in lines) {
            val first = board[a.first][a.second]
            if (first != 0 &&
                first == board[b.first][b.second] &&
                first == board[c.first][c.second]
            ) {
                return first
            }
        }
        return null
    }

    private fun freeCells(board: List<List<Int>>): List<Pair<Int, Int>> =
        buildList {
            for (i in 0 until 3)
                for (j in 0 until 3)
                    if (board[i][j] == 0) add(i to j)
        }
}
