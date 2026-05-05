package com.dgurnick.openuiexplore

import android.app.Application
import com.dgurnick.openuiexplore.di.networkModule
import com.dgurnick.openuiexplore.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpenUIApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Default backend URL. Adjust in settings for physical devices.
        // Emulator: 10.0.2.2 maps to the host machine's localhost.
        val backendUrl = "http://10.0.2.2:7878"

        startKoin {
            androidContext(this@OpenUIApp)
            modules(networkModule(backendUrl), viewModelModule)
        }
    }
}
