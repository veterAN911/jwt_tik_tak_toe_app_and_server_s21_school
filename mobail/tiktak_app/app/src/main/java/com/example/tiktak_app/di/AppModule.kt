package com.example.tiktak_app.di

import android.content.Context
import androidx.room.Room
import com.example.tiktak_app.datasource.database.AppDatabase
import com.example.tiktak_app.datasource.database.dao.GameDao
import com.example.tiktak_app.datasource.database.dao.UserDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val context: Context) {

    @Provides
    @Singleton
    fun provideContext(): Context = context

    @Provides
    @Singleton
    fun provideDatabase(context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "tiktak_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideGameDao(database: AppDatabase): GameDao = database.gameDao()
}
