package web.socket

import domain.model.Game
import domain.model.GameMode
import domain.model.GameStatus
import domain.service.GameService
import domain.service.JwtProvider
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import web.mapper.toDomain
import web.mapper.toWeb
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GameSocketHandler(
    private val gameService: GameService,
    private val jwtProvider: JwtProvider
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val sessions = ConcurrentHashMap<UUID, MutableSet<DefaultWebSocketServerSession>>()

    suspend fun handle(session: DefaultWebSocketServerSession) {
        val userId = session.userIdOrNull(jwtProvider)
        if (userId == null) {
            send(session, WsErrorMessage(message = "Недействительный токен"))
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return
        }

        var currentGameId: UUID? = null

        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue

                val request = try {
                    json.decodeFromString<WsRequest>(frame.readText())
                } catch (e: Exception) {
                    send(session, WsErrorMessage(message = "Некорректное сообщение"))
                    continue
                }

                try {
                    when (request.type.lowercase()) {
                        "create" -> {
                            val game = when (request.mode?.uppercase()) {
                                "HUMAN" -> gameService.createNewHumanGame(userId)
                                else -> gameService.createNewGame(userId)
                            }
                            currentGameId = game.id
                            subscribe(game.id, session)
                            sendGame(session, game)
                        }

                        "join" -> {
                            val gameId = parseGameId(request.gameId)
                                ?: throw IllegalArgumentException("Некорректный gameId")

                            val game = gameService.joinGame(gameId, userId)
                            currentGameId = gameId
                            subscribe(gameId, session)
                            broadcast(game)
                        }

                        "move" -> {
                            val gameId = parseGameId(request.gameId)
                                ?: throw IllegalArgumentException("Некорректный gameId")

                            val game = gameService.getGame(gameId)
                                ?: throw IllegalArgumentException("Игра не найдена")

                            requireParticipant(game, userId)

                            if (game.status != GameStatus.IN_PROGRESS) {
                                throw IllegalArgumentException("Игра уже не активна (status=${game.status})")
                            }

                            val board = request.board?.toDomain()
                                ?: throw IllegalArgumentException("Не передана доска")

                            gameService.validateMove(game, board)
                            gameService.updateActivity(gameId, userId)

                            var updated = gameService.applyMove(game, board)

                            if (updated.mode == GameMode.BOT &&
                                updated.status == GameStatus.IN_PROGRESS &&
                                updated.currentPlayer == updated.playerO
                            ) {
                                updated = gameService.makeBotMove(updated)
                            }

                            gameService.saveGame(updated)
                            currentGameId = gameId
                            subscribe(gameId, session)
                            broadcast(updated)
                        }

                        "get" -> {
                            val gameId = parseGameId(request.gameId)
                                ?: throw IllegalArgumentException("Некорректный gameId")

                            val game = gameService.getGame(gameId)
                                ?: throw IllegalArgumentException("Игра не найдена")

                            requireParticipant(game, userId)

                            currentGameId = gameId
                            subscribe(gameId, session)
                            sendGame(session, game)
                        }

                        "my" -> {
                            val games = gameService.getActiveGamesForUser(userId)
                            send(
                                session,
                                WsGamesMessage(games = games.map { it.toWeb() })
                            )
                        }

                        "unwatch" -> {
                            currentGameId?.let { id ->
                                sessions[id]?.remove(session)

                                gameService.cancelWaitingGame(id, userId)
                            }
                            currentGameId = null
                        }

                        else -> send(
                            session,
                            WsErrorMessage(message = "Неизвестный тип сообщения: ${request.type}")
                        )
                    }
                } catch (e: IllegalArgumentException) {
                    send(session, WsErrorMessage(message = e.message ?: "Ошибка игры"))
                }
            }
        } catch (_: ClosedReceiveChannelException) {

        } catch (e: CancellationException) {
            throw e
        } finally {
            sessions.values.forEach { it.remove(session) }
        }
    }

    private fun subscribe(gameId: UUID, session: DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(gameId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    private suspend fun broadcast(game: Game) {
        val text = json.encodeToString(WsGameMessage(game = game.toWeb()))

        sessions[game.id]?.toList()?.forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (_: Exception) {
                sessions[game.id]?.remove(session)
            }
        }
    }

    private suspend fun sendGame(session: DefaultWebSocketServerSession, game: Game) {
        send(session, WsGameMessage(game = game.toWeb()))
    }

    private suspend fun send(session: DefaultWebSocketServerSession, message: Any) {
        val text = when (message) {
            is WsGameMessage -> json.encodeToString(message)
            is WsGamesMessage -> json.encodeToString(message)
            is WsErrorMessage -> json.encodeToString(message)
            else -> return
        }

        session.send(Frame.Text(text))
    }

    private fun requireParticipant(game: Game, userId: UUID) {
        if (userId != game.playerX && userId != game.playerO) {
            throw IllegalArgumentException("Вы не участвуете в этой игре")
        }
    }
}
