package web.socket

import domain.model.GameStatus
import domain.service.GameService
import domain.service.JwtProvider
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import web.mapper.toWeb
import java.util.UUID

class TimerSocketHandler(
    private val gameService: GameService,
    private val jwtProvider: JwtProvider,
    private val tickMs: Long = 1000L
) {

    private val json = Json { encodeDefaults = true }

    suspend fun handle(session: DefaultWebSocketServerSession) {
        val userId = session.userIdOrNull(jwtProvider)
        if (userId == null) {
            session.send(
                Frame.Text(json.encodeToString(WsErrorMessage(message = "Недействительный токен")))
            )
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return
        }

        val knownGameIds = mutableSetOf<UUID>()

        try {
            while (session.isActive && !session.incoming.isClosedForReceive) {
                val games = gameService.getActiveGamesForUser(userId)
                val currentIds = games.mapTo(mutableSetOf()) { it.id }

                for (finishedId in knownGameIds - currentIds) {
                    val game = gameService.getGame(finishedId) ?: continue
                    if (game.status != GameStatus.FINISHED && game.status != GameStatus.DRAW) continue

                    val timer = gameService.getGameTimer(finishedId) ?: continue
                    session.send(Frame.Text(json.encodeToString(timer.toWeb(game.winner))))
                }

                for (game in games) {
                    val timer = gameService.getGameTimer(game.id) ?: continue
                    session.send(Frame.Text(json.encodeToString(timer.toWeb())))
                }

                knownGameIds.clear()
                knownGameIds.addAll(currentIds)

                delay(tickMs)
            }
        } catch (_: ClosedReceiveChannelException) {

        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {

        }
    }
}
