package com.dgurnick.openuiexplore.data.repository

import com.dgurnick.openuiexplore.data.model.ApiChatMessage
import com.dgurnick.openuiexplore.data.model.ChatMessage
import com.dgurnick.openuiexplore.data.model.Role
import com.dgurnick.openuiexplore.data.network.OpenUIApiService
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val apiService: OpenUIApiService) {
    fun streamResponse(history: List<ChatMessage>, model: String): Flow<String> {
        // Drop any leading assistant messages (e.g. the welcome/status message) before
        // the first user message — they confuse the LLM and override the system prompt persona.
        val firstUserIdx = history.indexOfFirst { it.role == Role.USER }
        val conversationHistory = if (firstUserIdx > 0) history.drop(firstUserIdx) else history

        val apiMessages =
                conversationHistory.map { msg ->
                    ApiChatMessage(
                            role =
                                    when (msg.role) {
                                        Role.USER -> "user"
                                        Role.ASSISTANT -> "assistant"
                                    },
                            content = msg.content
                    )
                }
        return apiService.streamChat(apiMessages, model)
    }
}
