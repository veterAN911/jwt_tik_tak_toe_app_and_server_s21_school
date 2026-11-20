package datasource.repository

import datasource.table.UsersTable
import domain.model.User
import domain.repository.UserRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserRepositoryImpl : UserRepository {

    override fun create(user: User): Boolean = transaction {
        UsersTable.insert {
            it[id] = user.id
            it[login] = user.login
            it[passwordHash] = user.passwordHash
        }
        true
    }

    override fun findByLogin(login: String): User? = transaction {
        UsersTable.select { UsersTable.login eq login }
            .map {
                User(
                    id = it[UsersTable.id].value,
                    login = it[UsersTable.login],
                    passwordHash = it[UsersTable.passwordHash],
                    botDifficulty = it[UsersTable.botDifficulty]
                )
            }
            .singleOrNull()
    }

    override fun findById(id: UUID): User? = transaction {
        UsersTable.select { UsersTable.id eq id }
            .map {
                User(
                    id = it[UsersTable.id].value,
                    login = it[UsersTable.login],
                    passwordHash = it[UsersTable.passwordHash],
                    botDifficulty = it[UsersTable.botDifficulty]
                )
            }
            .singleOrNull()
    }

    override fun updateBotDifficulty(id: UUID, difficulty: Double): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq id }) {
            it[botDifficulty] = difficulty
        } > 0
    }
}
