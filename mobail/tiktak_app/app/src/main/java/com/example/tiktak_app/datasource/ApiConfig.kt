package com.example.tiktak_app.datasource

import com.example.tiktak_app.BuildConfig

object ApiConfig {
    private const val HOST = BuildConfig.API_HOST
    const val BASE_URL = "http://$HOST/"
    const val WS_URL = "ws://$HOST/ws/game"
}
