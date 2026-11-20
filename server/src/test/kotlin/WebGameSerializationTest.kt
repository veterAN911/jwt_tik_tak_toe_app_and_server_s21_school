package test

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import web.model.WebBoard
import web.model.WebGame
import web.model.WebTimer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebGameSerializationTest {

    private val json = Json { encodeDefaults = true }

    private fun game(botDifficulty: String?) = WebGame(
        id = "game-id",
        board = WebBoard(List(3) { List(3) { 0 } }),
        mode = if (botDifficulty == null) "HUMAN" else "BOT",
        status = "IN_PROGRESS",
        currentPlayer = "player-x",
        winner = null,
        playerX = "player-x",
        playerO = "player-o",
        createdAt = 1L,
        botDifficulty = botDifficulty
    )

    @Test
    fun botGameContainsDifficultyLevel() {
        val body = json.encodeToString(game("LEVEL_3"))
        assertTrue(body.contains("\"botDifficulty\":\"LEVEL_3\""), body)
    }

    @Test
    fun humanGameOmitsDifficulty() {
        val body = json.encodeToString(game(null))
        assertFalse(body.contains("botDifficulty"), body)
    }

    @Test
    fun moveTimerOmitsWinner() {
        val body = json.encodeToString(
            WebTimer(gameId = "g", status = "IN_PROGRESS", remaining = 10, total = 40)
        )
        assertFalse(body.contains("winner"), body)
    }

    @Test
    fun finalTimerContainsWinner() {
        val body = json.encodeToString(
            WebTimer(gameId = "g", status = "FINISHED", remaining = 0, total = 40, winner = "p1")
        )
        assertTrue(body.contains("\"winner\":\"p1\""), body)
    }
}
