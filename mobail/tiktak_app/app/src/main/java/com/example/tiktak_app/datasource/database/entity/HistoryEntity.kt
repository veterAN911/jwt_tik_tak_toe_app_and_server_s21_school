package com.example.tiktak_app.datasource.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    val gameId: String,
    val createdAt: Long,
    val opponentLogin: String,
    val result: String
)
