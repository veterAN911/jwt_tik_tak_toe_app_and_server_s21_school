package di

import datasource.repository.GameRepositoryImpl
import datasource.repository.UserRepositoryImpl
import domain.repository.GameRepository
import domain.repository.UserRepository
import domain.service.*
import org.koin.dsl.module
import web.socket.GameSocketHandler
import web.socket.TimerSocketHandler

val appModule = module {

    single<UserRepository> { UserRepositoryImpl() }
    single<UserService> { UserServiceImpl(get()) }
    single {
        JwtProvider(
            secretAccess = "TEST_SECRET_ACCESS_1234567890123456",
            secretRefresh = "TEST_SECRET_REFRESH_1234567890123456"
        )
    }
    single<AuthService> { AuthServiceJwt(get(), get()) }

    single<GameRepository> { GameRepositoryImpl() }
    single<GameService> { GameServiceImpl(get(), get()) }

    single { GameSocketHandler(get(), get()) }
    single { TimerSocketHandler(get(), get()) }
}
