package com.example.tiktak_app.datasource.network.dto

data class FinishedGameDto(
    val gameId: String,
    val createdAt: Long,
    val opponentLogin: String,
    val result: String
)
