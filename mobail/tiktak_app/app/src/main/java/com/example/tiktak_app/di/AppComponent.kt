package com.example.tiktak_app.di

import androidx.lifecycle.ViewModelProvider
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        NetworkModule::class,
        RepositoryModule::class,
        ViewModelModule::class
    ]
)
interface AppComponent {
    fun viewModelFactory(): ViewModelProvider.Factory
}
