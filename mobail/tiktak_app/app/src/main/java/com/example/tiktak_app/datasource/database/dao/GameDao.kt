package com.example.tiktak_app.datasource.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(items: List<HistoryEntity>)

    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    suspend fun getHistory(): List<HistoryEntity>

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLeaderboard(items: List<LeaderboardEntity>)

    @Query("SELECT * FROM leaderboard ORDER BY position ASC")
    suspend fun getLeaderboard(): List<LeaderboardEntity>

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}
