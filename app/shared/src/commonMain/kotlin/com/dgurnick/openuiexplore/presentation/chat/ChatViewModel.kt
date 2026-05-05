package com.dgurnick.openuiexplore.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dgurnick.openuiexplore.data.model.ChatMessage
import com.dgurnick.openuiexplore.data.model.assistantMessage
import com.dgurnick.openuiexplore.data.model.userMessage
import com.dgurnick.openuiexplore.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val model: String = "gpt-4o-mini"
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun sendMessage(content: String) {
        if (_state.value.isStreaming || content.isBlank()) return

        val userMsg = userMessage(content)
        val streamingPlaceholder = assistantMessage("", isStreaming = true)

        _state.update {
            it.copy(
                messages = it.messages + userMsg + streamingPlaceholder,
                isStreaming = true,
                error = null
            )
        }

        viewModelScope.launch {
            runCatching {
                // Pass history excluding the empty placeholder
                val history = _state.value.messages.dropLast(1)
                chatRepository.streamResponse(history, model).collect { token ->
                    _state.update { state ->
                        val updated = state.messages.toMutableList()
                        val last = updated[updated.lastIndex]
                        updated[updated.lastIndex] = last.copy(content = last.content + token)
                        state.copy(messages = updated)
                    }
                }
                _state.update { state ->
                    val updated = state.messages.toMutableList()
                    updated[updated.lastIndex] = updated[updated.lastIndex].copy(isStreaming = false)
                    state.copy(messages = updated, isStreaming = false)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isStreaming = false,
                        error = e.message ?: "Streaming failed"
                    )
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
