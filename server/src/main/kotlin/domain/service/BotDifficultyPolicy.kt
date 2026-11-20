package domain.service

import domain.model.BotDifficultyLevel
import kotlin.math.round

object BotDifficultyPolicy {

    fun clamp(value: Double): Double =
        value.coerceIn(BotDifficultyLevel.MIN, BotDifficultyLevel.MAX)

    fun adjust(current: Double, humanWon: Boolean): Double {
        val delta = if (humanWon) BotDifficultyLevel.STEP else -BotDifficultyLevel.STEP
        return roundToStep(clamp(current) + delta)
    }

    fun toLevel(difficulty: Double): BotDifficultyLevel {
        val step = round(difficulty * 10.0).toInt()
            .coerceIn(1, BotDifficultyLevel.entries.size)
        return BotDifficultyLevel.entries[step - 1]
    }

    private fun roundToStep(value: Double): Double =
        clamp(round(value * 10.0) / 10.0)
}
