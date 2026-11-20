package datasource

import io.ktor.server.application.Application

fun Application.configureDatabase() {
    DbPostgres.init(
        url = "jdbc:postgresql://localhost:5432/tictactoe",
        user = "postgres",
        password = "31744464"
    )
}
