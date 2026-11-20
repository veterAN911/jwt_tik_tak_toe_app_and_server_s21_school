package com.example.tiktak_app.datasource.database

import com.example.tiktak_app.datasource.database.dao.GameDao
import com.example.tiktak_app.datasource.database.dao.UserDao
import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.database.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseDataSource @Inject constructor(
    private val userDao: UserDao,
    private val gameDao: GameDao
) {

    suspend fun saveUser(user: UserEntity) = userDao.saveUser(user)
    suspend fun getUser(): UserEntity? = userDao.getUser()

    suspend fun saveTokens(tokens: TokenEntity) = userDao.saveTokens(tokens)
    suspend fun getTokens(): TokenEntity? = userDao.getTokens()

    suspend fun replaceHistory(items: List<HistoryEntity>) {
        gameDao.clearHistory()
        gameDao.saveHistory(items)
    }

    suspend fun getHistory(): List<HistoryEntity> = gameDao.getHistory()

    suspend fun replaceLeaderboard(items: List<LeaderboardEntity>) {
        gameDao.clearLeaderboard()
        gameDao.saveLeaderboard(items)
    }

    suspend fun getLeaderboard(): List<LeaderboardEntity> = gameDao.getLeaderboard()

    suspend fun clearAll() {
        userDao.clearUsers()
        userDao.clearTokens()
        gameDao.clearHistory()
        gameDao.clearLeaderboard()
    }
}
