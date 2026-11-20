package domain.model

enum class BotDifficultyLevel(val value: Double) {
    LEVEL_1(0.1),
    LEVEL_2(0.2),
    LEVEL_3(0.3),
    LEVEL_4(0.4),
    LEVEL_5(0.5),
    LEVEL_6(0.6),
    LEVEL_7(0.7),
    LEVEL_8(0.8),
    LEVEL_9(0.9),
    LEVEL_10(1.0);

    companion object {
        const val MIN = 0.1
        const val MAX = 1.0
        const val STEP = 0.1
        const val DEFAULT = MIN
    }
}
