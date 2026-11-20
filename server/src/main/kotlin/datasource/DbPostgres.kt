package datasource

import datasource.table.GamesTable
import datasource.table.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import utils.Logger
import java.sql.DriverManager

object DbPostgres {

    fun init(url: String, user: String, password: String) {

        Logger.info("Инициализация PostgreSQL…")

        try {
            val sysUrl = "jdbc:postgresql://localhost:5432/postgres"
            Logger.info("Подключение к системной базе postgres…")

            DriverManager.getConnection(sysUrl, user, password).use { conn ->
                conn.createStatement().use { stmt ->

                    Logger.info("Проверка существования базы tictactoe…")

                    val rs = stmt.executeQuery(
                        "SELECT 1 FROM pg_database WHERE datname = 'tictactoe'"
                    )

                    if (!rs.next()) {
                        Logger.warn("База tictactoe не найдена. Создаю…")
                        stmt.executeUpdate("CREATE DATABASE tictactoe")
                        Logger.info("База tictactoe успешно создана.")
                    } else {
                        Logger.info("База tictactoe уже существует.")
                    }
                }
            }

            Logger.info("Подключение к базе tictactoe…")

            Database.connect(
                url = url,
                driver = "org.postgresql.Driver",
                user = user,
                password = password
            )

            Logger.info("Создание таблиц (если отсутствуют)…")

            transaction {
                SchemaUtils.create(UsersTable, GamesTable)

                exec(
                    "ALTER TABLE users ADD COLUMN IF NOT EXISTS bot_difficulty " +
                            "DOUBLE PRECISION NOT NULL DEFAULT 0.1"
                )

                exec(
                    "ALTER TABLE games ADD COLUMN IF NOT EXISTS bot_difficulty VARCHAR(10)"
                )
            }

            Logger.info("Таблицы успешно созданы или уже существуют.")

            transaction {
                exec(
                    """
                    UPDATE games
                    SET status = 'FINISHED',
                        winner = CASE WHEN current_player = player_x THEN player_o ELSE player_x END,
                        current_player = NULL
                    WHERE status = 'INTERRUPTED'
                      AND player_o IS NOT NULL
                      AND current_player IS NOT NULL
                      AND current_player IN (player_x, player_o);
                    """.trimIndent()
                )

                exec("DELETE FROM games WHERE status = 'INTERRUPTED'")

                exec("DROP FUNCTION IF EXISTS check_game_activity()")
            }

            Logger.info("Миграция статусов игр завершена.")

        } catch (e: Exception) {
            Logger.error("Ошибка инициализации БД: ${e.message}", e)
            throw e
        }
    }
}
