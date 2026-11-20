package com.example.tiktak_app.datasource.network.dto

data class BoardDto(
    val cells: List<List<Int>>
)

data class GameDto(
    val id: String,
    val board: BoardDto,
    val mode: String,
    val status: String,
    val currentPlayer: String?,
    val winner: String?,
    val playerX: String,
    val playerO: String?
)

data class WsRequestDto(
    val type: String,
    val mode: String? = null,
    val gameId: String? = null,
    val board: BoardDto? = null
)

data class WsMessageDto(
    val type: String?,
    val game: GameDto? = null,
    val message: String? = null
)
