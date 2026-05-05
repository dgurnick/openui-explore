package com.dgurnick.openuiexplore.data.repository

import com.dgurnick.openuiexplore.data.model.ApiChatMessage
import com.dgurnick.openuiexplore.data.model.ChatMessage
import com.dgurnick.openuiexplore.data.model.Role
import com.dgurnick.openuiexplore.data.network.OpenUIApiService
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val apiService: OpenUIApiService) {
    fun streamResponse(history: List<ChatMessage>, model: String): Flow<String> {
        val apiMessages = history.map { msg ->
            ApiChatMessage(
                role = when (msg.role) {
                    Role.USER -> "user"
                    Role.ASSISTANT -> "assistant"
                },
                content = msg.content
            )
        }
        return apiService.streamChat(apiMessages, model)
    }
}
