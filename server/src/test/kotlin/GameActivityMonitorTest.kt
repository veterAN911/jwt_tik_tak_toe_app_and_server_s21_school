package test

import utils.GameActivityMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertTrue

class GameActivityMonitorTest {

    @Test
    fun monitorRunsCheckRepeatedlyAndSurvivesExceptions() = runBlocking {
        val calls = AtomicInteger(0)

        val monitor = GameActivityMonitor(intervalMs = 20L) {
            val call = calls.incrementAndGet()
            if (call == 1) {
                throw IllegalStateException("временная ошибка БД")
            }
        }

        monitor.start()
        try {
            delay(200L)
        } finally {
            monitor.stop()
        }

        assertTrue(
            calls.get() >= 3,
            "Монитор должен продолжать работать после ошибки, вызовов: ${calls.get()}"
        )
    }
}
