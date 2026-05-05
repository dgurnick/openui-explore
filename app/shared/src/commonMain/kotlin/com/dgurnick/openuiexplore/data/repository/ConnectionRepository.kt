package com.dgurnick.openuiexplore.data.repository

import com.dgurnick.openuiexplore.data.network.OpenUIApiService

class ConnectionRepository(private val apiService: OpenUIApiService) {
    suspend fun checkConnection(): Boolean = apiService.checkHealth()
}
