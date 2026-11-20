package test

import domain.model.BotDifficultyLevel
import domain.service.BotAlgorithmGame
import domain.service.BotDifficultyPolicy
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotDifficultyTest {

    @Test
    fun difficultyChangesByStepAndStaysInRange() {
        assertEquals(0.2, BotDifficultyPolicy.adjust(0.1, humanWon = true), 1e-9)
        assertEquals(0.5, BotDifficultyPolicy.adjust(0.6, humanWon = false), 1e-9)
        assertEquals(1.0, BotDifficultyPolicy.adjust(0.9, humanWon = true), 1e-9)
        assertEquals(0.1, BotDifficultyPolicy.adjust(0.1, humanWon = false), 1e-9)
        assertEquals(1.0, BotDifficultyPolicy.adjust(1.0, humanWon = true), 1e-9)
    }

    @Test
    fun difficultyValueMapsToLevel() {
        assertEquals(BotDifficultyLevel.LEVEL_1, BotDifficultyPolicy.toLevel(0.1))
        assertEquals(BotDifficultyLevel.LEVEL_5, BotDifficultyPolicy.toLevel(0.5))
        assertEquals(BotDifficultyLevel.LEVEL_10, BotDifficultyPolicy.toLevel(1.0))
        assertEquals(BotDifficultyLevel.LEVEL_1, BotDifficultyPolicy.toLevel(0.0))
        assertEquals(BotDifficultyLevel.LEVEL_10, BotDifficultyPolicy.toLevel(2.0))
        assertEquals(10, BotDifficultyLevel.entries.size)
    }

    @Test
    fun perfectBotIsUnbeatable() {
        val random = Random(42)
        repeat(200) {
            val result = playGame(difficulty = 1.0, random = random)
            assertTrue(result != HUMAN, "Идеальный бот не должен проигрывать")
        }
    }

    @Test
    fun weakBotCanBeBeaten() {
        val random = Random(7)
        val humanWins = (1..200).count { playGame(difficulty = 0.1, random = random) == HUMAN }
        assertTrue(humanWins > 0, "Слабого бота должно быть можно обыграть")
    }

    private fun playGame(difficulty: Double, random: Random): Int {
        val board = MutableList(3) { MutableList(3) { 0 } }
        var humanTurn = true

        while (true) {
            winner(board)?.let { return it }
            val free = freeCells(board)
            if (free.isEmpty()) return 0

            val move = if (humanTurn) {
                free.random(random)
            } else {
                BotAlgorithmGame.chooseMove(board, BOT, HUMAN, difficulty, random)!!
            }
            board[move.first][move.second] = if (humanTurn) HUMAN else BOT
            humanTurn = !humanTurn
        }
    }

    private fun winner(board: List<List<Int>>): Int? {
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

    private companion object {
        const val HUMAN = 1
        const val BOT = 2
    }
}
