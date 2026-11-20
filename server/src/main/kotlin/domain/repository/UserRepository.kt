package domain.repository

import domain.model.User
import java.util.UUID

interface UserRepository {
    fun create(user: User): Boolean

    fun findByLogin(login: String): User?

    fun findById(id: UUID): User?

    fun updateBotDifficulty(id: UUID, difficulty: Double): Boolean
}
