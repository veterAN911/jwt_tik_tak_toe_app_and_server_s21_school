package com.example.tiktak_app.di

import com.example.tiktak_app.datasource.ApiConfig
import com.example.tiktak_app.datasource.network.api.ApiService
import com.example.tiktak_app.datasource.network.api.AuthApi
import com.example.tiktak_app.datasource.network.interceptor.AuthInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideAuthApi(client: OkHttpClient, gson: Gson): AuthApi =
        retrofit(client, gson).create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideApiService(
        client: OkHttpClient,
        interceptor: AuthInterceptor,
        gson: Gson
    ): ApiService {
        val authedClient = client.newBuilder()
            .addInterceptor(interceptor)
            .build()

        return retrofit(authedClient, gson).create(ApiService::class.java)
    }

    private fun retrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
}
