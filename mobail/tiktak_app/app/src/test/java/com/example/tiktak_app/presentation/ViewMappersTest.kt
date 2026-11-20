package com.example.tiktak_app.presentation

import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.presentation.mapper.toViewData
import com.example.tiktak_app.testFinishedGame
import com.example.tiktak_app.testLeaderboardPlayer
import com.example.tiktak_app.testUser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class ViewMappersTest {

    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `finished game result is translated to russian`() {
        assertEquals(
            "Победа",
            testFinishedGame(result = FinishedGame.RESULT_WIN).toViewData().resultText
        )
        assertEquals(
            "Поражение",
            testFinishedGame(result = FinishedGame.RESULT_LOSS).toViewData().resultText
        )
        assertEquals(
            "Ничья",
            testFinishedGame(result = FinishedGame.RESULT_DRAW).toViewData().resultText
        )
    }

    @Test
    fun `unknown result is passed as is`() {
        val item = testFinishedGame(result = "UNKNOWN").toViewData()

        assertEquals("UNKNOWN", item.resultText)
        assertEquals("UNKNOWN", item.result)
    }

    @Test
    fun `history view data keeps game and opponent`() {
        val item = testFinishedGame(gameId = "g-42", opponentLogin = "bot")
            .toViewData()

        assertEquals("g-42", item.gameId)
        assertEquals("bot", item.opponentLogin)
    }

    @Test
    fun `leaderboard view data formats ratio with two digits`() {
        assertEquals("1.50", testLeaderboardPlayer(winsRatio = 1.5).toViewData(1).winsRatio)
        assertEquals("0.00", testLeaderboardPlayer(winsRatio = 0.0).toViewData(2).winsRatio)
        assertEquals("2.25", testLeaderboardPlayer(winsRatio = 2.25).toViewData(3).winsRatio)
    }

    @Test
    fun `leaderboard view data keeps position and login`() {
        val item = testLeaderboardPlayer(login = "winner").toViewData(position = 7)

        assertEquals(7, item.position)
        assertEquals("winner", item.login)
    }

    @Test
    fun `user view data keeps id and login`() {
        val item = testUser(id = "u-9", login = "player9").toViewData()

        assertEquals("u-9", item.id)
        assertEquals("player9", item.login)
    }
}
