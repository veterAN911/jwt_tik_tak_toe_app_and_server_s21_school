package com.example.tiktak_app.datasource.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tiktak_app.datasource.database.dao.GameDao
import com.example.tiktak_app.datasource.database.dao.UserDao
import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TokenEntity::class,
        HistoryEntity::class,
        LeaderboardEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
}
