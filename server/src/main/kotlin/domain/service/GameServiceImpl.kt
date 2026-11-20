package domain.service

import domain.model.*
import domain.repository.GameRepository
import domain.repository.UserRepository
import utils.Logger
import java.util.*

class GameServiceImpl(
    private val repo: GameRepository,
    private val userRepo: UserRepository
) : GameService {

    private val BOT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    override fun createNewGame(playerX: UUID): Game {
        Logger.info("GameService.createNewGame: playerX=$playerX")
        return try {
            val board = Board(List(3) { List(3) { 0 } })

            val difficulty = BotDifficultyPolicy.toLevel(
                BotDifficultyPolicy.clamp(
                    userRepo.findById(playerX)?.botDifficulty ?: BotDifficultyLevel.DEFAULT
                )
            )

            val game = Game(
                id = UUID.randomUUID(),
                board = board,
                mode = GameMode.BOT,
                status = GameStatus.IN_PROGRESS,
                currentPlayer = playerX,
                winner = null,
                playerX = playerX,
                playerO = BOT_UUID,
                createdAt = System.currentTimeMillis(),
                botDifficulty = difficulty
            )

            Logger.info("GameService.createNewGame: created game ${game.id} mode=${game.mode} status=${game.status}")
            repo.save(game)
            repo.updateActivityX(game.id, game.createdAt)
            Logger.info("GameService.createNewGame: saved game ${game.id}")
            game
        } catch (e: Exception) {
            Logger.error("GameService.createNewGame: error for playerX=$playerX → ${e.message}", e)
            throw e
        }
    }

    override fun createNewHumanGame(playerX: UUID): Game {
        Logger.info("GameService.createNewHumanGame: playerX=$playerX")
        return try {
            val board = Board(List(3) { List(3) { 0 } })

            val game = Game(
                id = UUID.randomUUID(),
                board = board,
                mode = GameMode.HUMAN,
                status = GameStatus.WAITING,
                currentPlayer = playerX,
                winner = null,
                playerX = playerX,
                playerO = null,
                createdAt = System.currentTimeMillis()
            )

            Logger.info("GameService.createNewHumanGame: created game ${game.id} mode=${game.mode} status=${game.status}")
            repo.save(game)
            repo.updateActivityX(game.id, game.createdAt)
            Logger.info("GameService.createNewHumanGame: saved game ${game.id}")
            game
        } catch (e: Exception) {
            Logger.error("GameService.createNewHumanGame: error for playerX=$playerX → ${e.message}", e)
            throw e
        }
    }

    override fun joinGame(id: UUID, playerO: UUID): Game {
        Logger.info("GameService.joinGame: id=$id, playerO=$playerO")
        return try {
            val game = repo.get(id) ?: run {
                Logger.warn("GameService.joinGame: game $id not found")
                error("Game not found")
            }

            Logger.info("GameService.joinGame: found game ${game.id} status=${game.status} mode=${game.mode}")

            if (playerO == game.playerX) {
                Logger.info("GameService.joinGame: playerO=$playerO заходит в свою игру $id → возвращаем без изменений")
                return game
            }

            if (game.playerO != null) {
                Logger.warn("GameService.joinGame: игра $id уже имеет playerO=${game.playerO}")
                error("К этой игре уже подключился другой игрок")
            }

            val updated = game.copy(
                playerO = playerO,
                status = GameStatus.IN_PROGRESS
            )

            Logger.info("GameService.joinGame: updated game ${updated.id} playerO=$playerO status=${updated.status}")
            repo.save(updated)

            val now = System.currentTimeMillis()
            repo.updateActivityX(updated.id, now)
            repo.updateActivityO(updated.id, now)

            Logger.info("GameService.joinGame: saved updated game ${updated.id}")
            updated

        } catch (e: Exception) {
            Logger.error("GameService.joinGame: error for id=$id, playerO=$playerO → ${e.message}", e)
            throw e
        }
    }

    override fun getGame(id: UUID): Game? {
        Logger.info("GameService.getGame: id=$id")
        return try {
            val game = repo.get(id)
            if (game == null) {
                Logger.warn("GameService.getGame: game $id not found")
            } else {
                Logger.info("GameService.getGame: game ${game.id} status=${game.status} mode=${game.mode}")
            }
            game
        } catch (e: Exception) {
            Logger.error("GameService.getGame: error for id=$id → ${e.message}", e)
            throw e
        }
    }

    override fun saveGame(game: Game) {
        Logger.info("GameService.saveGame: id=${game.id} status=${game.status} mode=${game.mode}")
        try {
            repo.save(game)
            Logger.info("GameService.saveGame: saved game ${game.id}")
        } catch (e: Exception) {
            Logger.error("GameService.saveGame: error for id=${game.id} → ${e.message}", e)
            throw e
        }
    }

    override fun validateMove(oldGame: Game, newBoard: Board) {
        Logger.info("GameService.validateMove: gameId=${oldGame.id} status=${oldGame.status}")
        try {
            if (oldGame.status != GameStatus.IN_PROGRESS)
                throw IllegalArgumentException("Воу Воу, погоди немного. Явно ход не твой и не сейчас!")

            val oldCells = oldGame.board.cells
            val newCells = newBoard.cells

            var diff = 0
            var x = -1
            var y = -1

            for (i in 0 until 3)
                for (j in 0 until 3)
                    if (oldCells[i][j] != newCells[i][j]) {
                        diff++
                        x = i
                        y = j
                    }

            if (diff != 1)
                throw IllegalArgumentException("Ля ты крыса! Только одну ячейку можно изменить!")

            if (oldCells[x][y] != 0)
                throw IllegalArgumentException("Ты че удумал? Тут уже сходили АРА!")

            val expected = if (oldGame.currentPlayer == oldGame.playerX) 1 else 2
            if (newCells[x][y] != expected)
                throw IllegalArgumentException("Щас вроде противник должен ходить. Куда ты лезешь? Остановись, подумай.")

            Logger.info("GameService.validateMove: move is valid for gameId=${oldGame.id}")
        } catch (e: IllegalArgumentException) {
            Logger.warn("GameService.validateMove: invalid move for gameId=${oldGame.id} → ${e.message}")
            throw e
        } catch (e: Exception) {
            Logger.error("GameService.validateMove: error for gameId=${oldGame.id} → ${e.message}", e)
            throw e
        }
    }

    override fun applyMove(game: Game, newBoard: Board): Game {
        Logger.info("GameService.applyMove: gameId=${game.id} status=${game.status}")
        return try {
            val winner = detectWinner(game, newBoard)

            val updated = when {
                winner != null -> game.copy(
                    board = newBoard,
                    winner = winner,
                    status = GameStatus.FINISHED
                )

                newBoard.cells.flatten().none { it == 0 } -> game.copy(
                    board = newBoard,
                    winner = null,
                    status = GameStatus.DRAW
                )

                else -> {
                    val next = if (game.currentPlayer == game.playerX) game.playerO else game.playerX
                    game.copy(
                        board = newBoard,
                        currentPlayer = next,
                        status = GameStatus.IN_PROGRESS
                    )
                }
            }

            Logger.info(
                "GameService.applyMove: updated gameId=${updated.id} " +
                        "status=${updated.status} winner=${updated.winner} currentPlayer=${updated.currentPlayer}"
            )

            if (game.status != GameStatus.FINISHED &&
                updated.status == GameStatus.FINISHED &&
                updated.mode == GameMode.BOT
            ) {
                try {
                    updateBotDifficulty(updated)
                } catch (e: Exception) {
                    Logger.error(
                        "GameService.applyMove: не удалось обновить сложность бота " +
                                "для игрока ${updated.playerX} → ${e.message}", e
                    )
                }
            }

            updated
        } catch (e: Exception) {
            Logger.error("GameService.applyMove: error for gameId=${game.id} → ${e.message}", e)
            throw e
        }
    }

    private fun detectWinner(game: Game, board: Board): UUID? {
        Logger.info("GameService.detectWinner: gameId=${game.id}")
        val b = board.cells

        fun line(a: Int, b: Int, c: Int) = a != 0 && a == b && b == c

        val lines = listOf(
            Triple(b[0][0], b[0][1], b[0][2]),
            Triple(b[1][0], b[1][1], b[1][2]),
            Triple(b[2][0], b[2][1], b[2][2]),
            Triple(b[0][0], b[1][0], b[2][0]),
            Triple(b[0][1], b[1][1], b[2][1]),
            Triple(b[0][2], b[1][2], b[2][2]),
            Triple(b[0][0], b[1][1], b[2][2]),
            Triple(b[0][2], b[1][1], b[2][0])
        )

        for ((a, b1, c) in lines) {
            if (line(a, b1, c)) {
                val winner = if (a == 1) game.playerX else game.playerO
                Logger.info("GameService.detectWinner: winner=$winner in gameId=${game.id}")
                return winner
            }
        }

        Logger.info("GameService.detectWinner: no winner in gameId=${game.id}")
        return null
    }

    override fun getWaitingGames(): List<Game> {
        Logger.info("GameService.getWaitingGames: request")
        return try {
            val games = repo.getWaitingGames()
            Logger.info("GameService.getWaitingGames: found ${games.size} games")
            games
        } catch (e: Exception) {
            Logger.error("GameService.getWaitingGames: error → ${e.message}", e)
            throw e
        }
    }

    override fun getActiveGamesForUser(userId: UUID): List<Game> {
        Logger.info("GameService.getActiveGamesForUser: userId=$userId")
        return repo.getActiveGamesForUser(userId)
    }

    override fun getGameTimer(id: UUID): GameTimer? {
        val game = repo.get(id) ?: return null
        val activity = repo.getActivity(id) ?: return null

        return GameTimerCalculator.calculate(
            game = game,
            lastActivityX = activity.lastActivityX,
            lastActivityO = activity.lastActivityO,
            nowMs = System.currentTimeMillis()
        )
    }

    override fun makeBotMove(game: Game): Game {
        val board = game.board.cells.map { it.toMutableList() }.toMutableList()

        val bot = if (game.playerO == BOT_UUID) 2 else 1
        val human = if (bot == 1) 2 else 1

        val difficulty = game.botDifficulty?.value ?: BotDifficultyPolicy.clamp(
            userRepo.findById(game.playerX)?.botDifficulty ?: BotDifficultyLevel.DEFAULT
        )
        Logger.info("GameService.makeBotMove: gameId=${game.id} difficulty=$difficulty")

        val move = BotAlgorithmGame.chooseMove(board, bot, human, difficulty)
            ?: return game

        val (x, y) = move
        board[x][y] = bot

        return applyMove(game, Board(board))
    }

    private fun updateBotDifficulty(finished: Game) {
        val user = userRepo.findById(finished.playerX) ?: return

        val humanWon = when (finished.winner) {
            finished.playerX -> true
            finished.playerO -> false
            else -> return
        }

        val next = BotDifficultyPolicy.adjust(user.botDifficulty, humanWon)
        userRepo.updateBotDifficulty(finished.playerX, next)

        Logger.info(
            "GameService.updateBotDifficulty: userId=${finished.playerX} " +
                    "${user.botDifficulty} → $next (humanWon=$humanWon)"
        )
    }

    override fun updateActivity(gameId: UUID, playerId: UUID) {
        val game = repo.get(gameId) ?: return
        val ts = System.currentTimeMillis()

        when (playerId) {
            game.playerX -> repo.updateActivityX(gameId, ts)
            game.playerO -> repo.updateActivityO(gameId, ts)
            else -> Logger.warn("updateActivity: player $playerId не принадлежит игре $gameId")
        }
    }

    override fun cancelWaitingGame(gameId: UUID, userId: UUID) {
        val game = repo.get(gameId) ?: return

        if (game.status == GameStatus.WAITING && game.playerX == userId) {
            repo.delete(gameId)
            Logger.info("GameService.cancelWaitingGame: удалена ожидающая игра $gameId (playerX=$userId)")
        }
    }

    override fun getFinishedGamesHistory(userId: UUID): List<FinishedGameItem> {
        Logger.info("GameService.getFinishedGamesHistory: userId=$userId")
        return repo.getFinishedGamesHistory(userId)
    }

    override fun getTopPlayers(limit: Int): List<LeaderboardEntry> {
        Logger.info("GameService.getTopPlayers: limit=$limit")
        return try {
            val list = repo.getTopPlayers(limit)
            Logger.info("GameService.getTopPlayers: found ${list.size} players")
            list
        } catch (e: Exception) {
            Logger.error("GameService.getTopPlayers: error → ${e.message}", e)
            throw e
        }
    }

    override fun finishTimedOutGames(nowMs: Long) {
        for (game in repo.getInProgressGames()) {
            try {
                val activity = repo.getActivity(game.id) ?: continue

                val finished = GameTimeout.resolve(
                    game = game,
                    lastActivityX = activity.lastActivityX,
                    lastActivityO = activity.lastActivityO,
                    nowMs = nowMs
                ) ?: continue

                repo.save(finished)

                Logger.info(
                    "GameService.finishTimedOutGames: gameId=${finished.id} " +
                            "status=${finished.status} winner=${finished.winner}"
                )

                if (finished.mode == GameMode.BOT) {
                    updateBotDifficulty(finished)
                }
            } catch (e: Exception) {
                Logger.error(
                    "GameService.finishTimedOutGames: ошибка для игры ${game.id} → ${e.message}", e
                )
            }
        }
    }

}
