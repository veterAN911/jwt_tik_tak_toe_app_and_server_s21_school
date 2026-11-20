package domain.service

import domain.model.User
import java.util.*

interface UserService {

    fun createUser(login: String, passwordHash: String): Boolean

    fun getUserByLogin(login: String): User?

    fun getUserById(id: UUID): User?
}
