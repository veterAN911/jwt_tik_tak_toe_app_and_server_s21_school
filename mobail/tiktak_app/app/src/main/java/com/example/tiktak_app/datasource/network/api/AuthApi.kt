package com.example.tiktak_app.datasource.network.api

import com.example.tiktak_app.datasource.network.dto.LoginRequestDto
import com.example.tiktak_app.datasource.network.dto.RefreshRequestDto
import com.example.tiktak_app.datasource.network.dto.TokenResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<TokenResponseDto>

    @POST("/auth/signup")
    suspend fun signUp(@Body body: LoginRequestDto): Response<Unit>

    @POST("/auth/refresh/access")
    suspend fun refreshAccess(@Body body: RefreshRequestDto): Response<TokenResponseDto>
}
