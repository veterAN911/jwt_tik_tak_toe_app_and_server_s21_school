package test

import domain.model.Board
import domain.model.FinishedGameItem
import domain.model.Game
import domain.model.GameMode
import domain.model.GameStatus
import domain.model.GameTimer
import domain.service.GameTimerCalculator
import domain.model.LeaderboardEntry
import domain.model.User
import domain.service.GameService
import domain.service.JwtProvider
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import web.socket.GameSocketHandler
import web.socket.TimerSocketHandler
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameWebSocketTest {

    private val jwtProvider = JwtProvider(
        secretAccess = "TEST_SECRET_ACCESS_1234567890123456",
        secretRefresh = "TEST_SECRET_REFRESH_1234567890123456"
    )

    private val user = User(login = "ws-user", passwordHash = "hash")

    @Test
    fun createGameAndMakeMoveOverGameSocket() = testApplication {
        val fake = FakeGameService()

        environment { config = MapApplicationConfig() }
        application {
            install(ServerWebSockets)
            routing {
                webSocket("/ws/game") { GameSocketHandler(fake, jwtProvider).handle(this) }
            }
        }

        val wsClient = createClient { install(ClientWebSockets) }
        val session = wsClient.webSocketSession("/ws/game?token=${jwtProvider.generateAccessToken(user)}")

        try {
            session.send(Frame.Text("""{"type":"create","mode":"BOT"}"""))

            val created = receiveGame(session)
            val gameId = created["id"]!!.jsonPrimitive.content
            assertEquals("BOT", created["mode"]!!.jsonPrimitive.content)
            assertEquals("IN_PROGRESS", created["status"]!!.jsonPrimitive.content)

            session.send(
                Frame.Text(
                    """{"type":"move","gameId":"$gameId","board":{"cells":[[1,0,0],[0,0,0],[0,0,0]]}}"""
                )
            )

            val moved = receiveGame(session)
            assertEquals(gameId, moved["id"]!!.jsonPrimitive.content)

            val firstCell = moved["board"]!!.jsonObject["cells"]!!.jsonArray[0].jsonArray[0]
            assertEquals(1, firstCell.jsonPrimitive.int)
        } finally {
            session.close()
        }
    }

    @Test
    fun timerSocketStreamsGameIdAndRemainingTime() = testApplication {
        val fake = FakeGameService()

        environment { config = MapApplicationConfig() }
        application {
            install(ServerWebSockets)
            routing {
                webSocket("/ws/game") { GameSocketHandler(fake, jwtProvider).handle(this) }
                webSocket("/ws/timer") {
                    TimerSocketHandler(fake, jwtProvider, tickMs = 20L).handle(this)
                }
            }
        }

        val wsClient = createClient { install(ClientWebSockets) }
        val token = jwtProvider.generateAccessToken(user)

        val gameSession = wsClient.webSocketSession("/ws/game?token=$token")
        gameSession.send(Frame.Text("""{"type":"create","mode":"BOT"}"""))
        val gameId = receiveGame(gameSession)["id"]!!.jsonPrimitive.content

        val timerSession = wsClient.webSocketSession("/ws/timer?token=$token")

        try {
            val timer = withTimeout(2_000L) { receiveTimer(timerSession) }

            assertEquals("timer", timer["type"]!!.jsonPrimitive.content)
            assertEquals(gameId, timer["gameId"]!!.jsonPrimitive.content)
            assertEquals("IN_PROGRESS", timer["status"]!!.jsonPrimitive.content)

            val remaining = timer["remaining"]!!.jsonPrimitive.long
            assertTrue(remaining in 0L..40L, "remaining=$remaining")
        } finally {
            timerSession.close()
            gameSession.close()
        }
    }

    @Test
    fun invalidTokenIsRejected() = testApplication {
        val fake = FakeGameService()

        environment { config = MapApplicationConfig() }
        application {
            install(ServerWebSockets)
            routing {
                webSocket("/ws/game") { GameSocketHandler(fake, jwtProvider).handle(this) }
            }
        }

        val wsClient = createClient { install(ClientWebSockets) }
        val session = wsClient.webSocketSession("/ws/game?token=invalid")

        val frame = withTimeout(2_000L) { session.incoming.receive() }
        assertTrue(
            (frame as Frame.Text).readText().contains("Недействительный токен"),
            "Сервер должен сообщить о неверном токене"
        )

        session.close()
    }

    private suspend fun receiveGame(session: DefaultClientWebSocketSession): kotlinx.serialization.json.JsonObject {
        return when (val message = receiveMessage(session)) {
            is WsReply.Game -> message.game
            is WsReply.Timer -> receiveGame(session)
            is WsReply.Error -> error("WS error: ${message.message}")
        }
    }

    private suspend fun receiveTimer(session: DefaultClientWebSocketSession): kotlinx.serialization.json.JsonObject {
        return when (val message = receiveMessage(session)) {
            is WsReply.Timer -> message.body
            is WsReply.Game -> receiveTimer(session)
            is WsReply.Error -> error("WS error: ${message.message}")
        }
    }

    private suspend fun receiveMessage(session: DefaultClientWebSocketSession): WsReply {
        while (true) {
            val frame = session.incoming.receive()

            if (frame !is Frame.Text) continue

            val body = Json.parseToJsonElement(frame.readText()).jsonObject

            when (body["type"]?.jsonPrimitive?.content) {
                "game" -> return WsReply.Game(body["game"]!!.jsonObject)
                "error" -> return WsReply.Error(body["message"]?.jsonPrimitive?.content ?: "unknown")
                "timer" -> return WsReply.Timer(body)
            }
        }
    }

    private sealed class WsReply {
        data class Game(val game: kotlinx.serialization.json.JsonObject) : WsReply()
        data class Timer(val body: kotlinx.serialization.json.JsonObject) : WsReply()
        data class Error(val message: String) : WsReply()
    }

    private class FakeGameService : GameService {

        private val botId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val games = mutableMapOf<UUID, Game>()
        private val lastActivity = mutableMapOf<UUID, Long>()

        override fun createNewGame(playerX: UUID): Game =
            create(playerX, GameMode.BOT, GameStatus.IN_PROGRESS, botId)

        override fun createNewHumanGame(playerX: UUID): Game =
            create(playerX, GameMode.HUMAN, GameStatus.WAITING, null)

        private fun create(
            playerX: UUID,
            mode: GameMode,
            status: GameStatus,
            playerO: UUID?
        ): Game {
            val now = System.currentTimeMillis()

            val game = Game(
                id = UUID.randomUUID(),
                board = Board(List(3) { List(3) { 0 } }),
                mode = mode,
                status = status,
                currentPlayer = playerX,
                winner = null,
                playerX = playerX,
                playerO = playerO,
                createdAt = now
            )

            games[game.id] = game
            lastActivity[game.id] = now
            return game
        }

        override fun joinGame(id: UUID, playerO: UUID): Game {
            val game = games[id] ?: error("Game not found")
            val updated = game.copy(playerO = playerO, status = GameStatus.IN_PROGRESS)
            games[id] = updated
            lastActivity[id] = System.currentTimeMillis()
            return updated
        }

        override fun getGame(id: UUID): Game? = games[id]

        override fun saveGame(game: Game) {
            games[game.id] = game
        }

        override fun getWaitingGames(): List<Game> =
            games.values.filter { it.status == GameStatus.WAITING }

        override fun getActiveGamesForUser(userId: UUID): List<Game> =
            games.values.filter {
                (it.playerX == userId || it.playerO == userId) &&
                        (it.status == GameStatus.WAITING || it.status == GameStatus.IN_PROGRESS)
            }

        override fun getGameTimer(id: UUID): GameTimer? {
            val game = games[id] ?: return null
            return GameTimerCalculator.calculate(
                game = game,
                lastActivityX = lastActivity[id] ?: 0L,
                lastActivityO = 0L,
                nowMs = System.currentTimeMillis()
            )
        }

        override fun validateMove(oldGame: Game, newBoard: Board) {
            if (oldGame.status != GameStatus.IN_PROGRESS) {
                throw IllegalArgumentException("Игра уже не активна")
            }

            var diff = 0
            for (i in 0 until 3) {
                for (j in 0 until 3) {
                    if (oldGame.board.cells[i][j] != newBoard.cells[i][j]) diff++
                }
            }

            if (diff != 1) throw IllegalArgumentException("Только одну ячейку можно изменить")
        }

        override fun applyMove(game: Game, newBoard: Board): Game {
            val full = newBoard.cells.flatten().none { it == 0 }
            val next = if (game.currentPlayer == game.playerX) game.playerO else game.playerX

            return game.copy(
                board = newBoard,
                currentPlayer = if (full) null else next,
                status = if (full) GameStatus.DRAW else GameStatus.IN_PROGRESS
            )
        }

        override fun makeBotMove(game: Game): Game = game

        override fun updateActivity(gameId: UUID, playerId: UUID) {
            lastActivity[gameId] = System.currentTimeMillis()
        }

        override fun cancelWaitingGame(gameId: UUID, userId: UUID) = Unit

        override fun getFinishedGamesHistory(userId: UUID): List<FinishedGameItem> = emptyList()

        override fun getTopPlayers(limit: Int): List<LeaderboardEntry> = emptyList()

        override fun finishTimedOutGames(nowMs: Long) = Unit
    }
}
