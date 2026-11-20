package domain.service

import utils.GameActivityMonitor
import utils.Logger

object GameTimeoutMonitor {

    private var monitor: GameActivityMonitor? = null

    @Synchronized
    fun start(gameService: GameService, intervalMs: Long = 1000L) {
        monitor?.stop()
        monitor = GameActivityMonitor(intervalMs = intervalMs) {
            gameService.finishTimedOutGames(System.currentTimeMillis())
        }.also { it.start() }

        Logger.info("GameTimeoutMonitor: запущен (intervalMs=$intervalMs)")
    }

    @Synchronized
    fun stop() {
        monitor?.stop()
        monitor = null
    }
}
