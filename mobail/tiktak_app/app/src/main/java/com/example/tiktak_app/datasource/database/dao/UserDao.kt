package com.example.tiktak_app.datasource.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.database.entity.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTokens(tokens: TokenEntity)

    @Query("SELECT * FROM tokens WHERE id = 1")
    suspend fun getTokens(): TokenEntity?

    @Query("DELETE FROM tokens")
    suspend fun clearTokens()
}
