package web.mapper

import domain.model.Board
import domain.model.Game
import domain.model.FinishedGameItem
import domain.model.GameTimer
import web.model.WebBoard
import web.model.WebGame
import web.model.WebTimer
import web.model.FinishedGameItemWeb

fun Game.toWeb(): WebGame =
    WebGame(
        id = id.toString(),
        board = WebBoard(board.cells),
        mode = mode.name,
        status = status.name,
        currentPlayer = currentPlayer?.toString(),
        winner = winner?.toString(),
        playerX = playerX.toString(),
        playerO = playerO?.toString(),
        createdAt = createdAt,
        botDifficulty = botDifficulty?.name
    )

fun GameTimer.toWeb(winner: java.util.UUID? = null): WebTimer =
    WebTimer(
        gameId = gameId.toString(),
        status = status.name,
        remaining = remaining,
        total = total,
        winner = winner?.toString()
    )

fun WebBoard.toDomain(): Board =
    Board(cells = this.cells)

fun FinishedGameItem.toWeb() = FinishedGameItemWeb(
    gameId = gameId.toString(),
    createdAt = createdAt,
    opponentLogin = opponentLogin,
    result = result
)
