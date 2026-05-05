package com.dgurnick.openuiexplore.bff.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(val token: String)

@Serializable
data class ErrorResponse(val message: String)
