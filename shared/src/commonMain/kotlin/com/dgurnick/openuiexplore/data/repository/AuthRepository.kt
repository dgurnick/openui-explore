package com.dgurnick.openuiexplore.data.repository

import com.dgurnick.openuiexplore.data.network.OpenUIApiService

class AuthRepository(private val apiService: OpenUIApiService) {

    suspend fun login(username: String, password: String): Result<String> = runCatching {
        val token = apiService.login(username, password)
        // Store the token in the service so all subsequent requests include it
        apiService.authToken = token
        token
    }

    fun logout() {
        apiService.authToken = null
    }
}
