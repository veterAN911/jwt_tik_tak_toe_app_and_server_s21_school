package com.example.tiktak_app

import android.app.Application
import com.example.tiktak_app.di.AppComponent
import com.example.tiktak_app.di.AppModule
import com.example.tiktak_app.di.DaggerAppComponent

class App : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}
