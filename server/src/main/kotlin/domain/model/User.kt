package domain.model

import java.util.UUID

data class User(
    val id: UUID = UUID.randomUUID(),
    val login: String,
    val passwordHash: String,
    val botDifficulty: Double = BotDifficultyLevel.DEFAULT
)
