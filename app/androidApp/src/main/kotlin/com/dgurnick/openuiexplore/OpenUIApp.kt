package com.dgurnick.openuiexplore

import android.app.Application
import com.dgurnick.openuiexplore.di.networkModule
import com.dgurnick.openuiexplore.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpenUIApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // BFF URL. Emulator: 10.0.2.2 maps to the host machine's localhost.
        // The BFF runs on port 8080 and proxies to the OpenUI backend on 7878.
        // For physical devices, replace with the LAN IP of your machine.
        val backendUrl = "http://10.0.2.2:8080"

        startKoin {
            androidContext(this@OpenUIApp)
            modules(networkModule(backendUrl), viewModelModule)
        }
    }
}
