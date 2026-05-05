package com.dgurnick.openuiexplore.di

import com.dgurnick.openuiexplore.data.network.OpenUIApiService
import com.dgurnick.openuiexplore.data.network.createHttpClient
import com.dgurnick.openuiexplore.data.repository.AuthRepository
import com.dgurnick.openuiexplore.data.repository.ChatRepository
import com.dgurnick.openuiexplore.data.repository.ConnectionRepository
import org.koin.dsl.module

fun networkModule(baseUrl: String) = module {
    single { createHttpClient() }
    single { OpenUIApiService(get(), baseUrl) }
    single { ConnectionRepository(get()) }
    single { AuthRepository(get()) }
    single { ChatRepository(get()) }
}
