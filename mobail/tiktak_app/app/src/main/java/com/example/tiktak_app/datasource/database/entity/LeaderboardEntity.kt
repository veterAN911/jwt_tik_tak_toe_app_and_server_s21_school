package com.example.tiktak_app.datasource.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntity(
    @PrimaryKey
    val userId: String,
    val login: String,
    val ratio: Double,
    val position: Int
)
