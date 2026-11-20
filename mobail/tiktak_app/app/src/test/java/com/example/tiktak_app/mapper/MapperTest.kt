package com.example.tiktak_app.mapper

import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.mapper.toDomain
import com.example.tiktak_app.datasource.mapper.toEntity
import com.example.tiktak_app.datasource.network.dto.BoardDto
import com.example.tiktak_app.datasource.network.dto.FinishedGameDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.LeaderboardDto
import com.example.tiktak_app.datasource.network.dto.TokenResponseDto
import com.example.tiktak_app.datasource.network.dto.UserDto
import com.example.tiktak_app.domain.model.AuthTokens
import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import com.example.tiktak_app.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    @Test
    fun `token response maps to domain tokens`() {
        val tokens = TokenResponseDto("access", "refresh").toDomain()

        assertEquals(AuthTokens("access", "refresh"), tokens)
    }

    @Test
    fun `user maps between dto entity and domain`() {
        val dto = UserDto(id = "id", login = "login")

        val user = dto.toDomain()
        assertEquals(User("id", "login"), user)
        assertEquals("id", user.toEntity().id)
        assertEquals("login", user.toEntity().login)
        assertEquals(User("id", "login"), user.toEntity().toDomain())
    }

    @Test
    fun `finished game maps between dto entity and domain`() {
        val dto = FinishedGameDto(
            gameId = "game",
            createdAt = 123L,
            opponentLogin = "enemy",
            result = FinishedGame.RESULT_LOSS
        )

        val game = dto.toDomain()
        assertEquals("game", game.gameId)
        assertEquals(123L, game.createdAt)
        assertEquals("enemy", game.opponentLogin)
        assertEquals(FinishedGame.RESULT_LOSS, game.result)
        assertEquals(game, game.toEntity().toDomain())
    }

    @Test
    fun `leaderboard maps between dto entity and domain`() {
        val dto = LeaderboardDto(userId = "u1", login = "top", ratio = 2.5)

        val player = dto.toDomain()
        assertEquals(LeaderboardPlayer("u1", "top", 2.5), player)

        val entity = LeaderboardEntity(
            userId = "u2",
            login = "second",
            ratio = 1.0,
            position = 1
        )
        assertEquals(LeaderboardPlayer("u2", "second", 1.0), entity.toDomain())
    }

    @Test
    fun `game dto maps board and players to domain`() {
        val cells = listOf(
            listOf(Game.SYMBOL_X, Game.EMPTY_CELL, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.SYMBOL_O, Game.EMPTY_CELL),
            listOf(Game.EMPTY_CELL, Game.EMPTY_CELL, Game.EMPTY_CELL)
        )
        val dto = GameDto(
            id = "game",
            board = BoardDto(cells),
            mode = Game.MODE_BOT,
            status = Game.STATUS_IN_PROGRESS,
            currentPlayer = "x-player",
            winner = null,
            playerX = "x-player",
            playerO = Game.BOT_ID
        )

        val game = dto.toDomain()
        assertEquals("game", game.id)
        assertEquals(cells, game.board)
        assertEquals(Game.MODE_BOT, game.mode)
        assertEquals("x-player", game.playerX)
        assertEquals(Game.BOT_ID, game.playerO)
    }

    @Test
    fun `history entity maps back to dto domain`() {
        val entity = HistoryEntity(
            gameId = "h1",
            createdAt = 10L,
            opponentLogin = "bot",
            result = FinishedGame.RESULT_DRAW
        )

        val game = entity.toDomain()
        assertEquals("h1", game.gameId)
        assertEquals(FinishedGame.RESULT_DRAW, game.result)
        assertEquals(entity, game.toEntity())
    }
}
