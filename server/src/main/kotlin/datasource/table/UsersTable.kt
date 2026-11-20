package datasource.table

import domain.model.BotDifficultyLevel
import org.jetbrains.exposed.dao.id.UUIDTable

object UsersTable : UUIDTable("users") {
    val login = varchar("login", 64).uniqueIndex()
    val passwordHash = text("password_hash")
    val botDifficulty = double("bot_difficulty").default(BotDifficultyLevel.DEFAULT)
}
