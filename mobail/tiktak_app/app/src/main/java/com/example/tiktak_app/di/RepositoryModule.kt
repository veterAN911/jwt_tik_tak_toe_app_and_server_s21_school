package com.example.tiktak_app.di

import com.example.tiktak_app.datasource.TokenManager
import com.example.tiktak_app.datasource.database.DatabaseDataSource
import com.example.tiktak_app.datasource.network.NetworkDataSource
import com.example.tiktak_app.datasource.network.socket.GameSocketClient
import com.example.tiktak_app.domain.repository.AuthRepository
import com.example.tiktak_app.domain.repository.AuthRepositoryImpl
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.domain.repository.GameRepositoryImpl
import com.example.tiktak_app.utils.JwtUtil
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        network: NetworkDataSource,
        database: DatabaseDataSource,
        tokenManager: TokenManager,
        jwtUtil: JwtUtil
    ): AuthRepository = AuthRepositoryImpl(network, database, tokenManager, jwtUtil)

    @Provides
    @Singleton
    fun provideGameRepository(
        network: NetworkDataSource,
        database: DatabaseDataSource,
        socketClient: GameSocketClient
    ): GameRepository = GameRepositoryImpl(network, database, socketClient)
}
