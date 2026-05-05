package com.dgurnick.openuiexplore.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dgurnick.openuiexplore.data.model.ChatMessage
import com.dgurnick.openuiexplore.data.model.assistantMessage
import com.dgurnick.openuiexplore.data.model.userMessage
import com.dgurnick.openuiexplore.data.network.OpenUIApiService
import com.dgurnick.openuiexplore.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickAction(val label: String, val prompt: String)

data class ChatState(
        val messages: List<ChatMessage> = emptyList(),
        val isStreaming: Boolean = false,
        val error: String? = null,
        val quickActions: List<QuickAction> = emptyList()
)

class ChatViewModel(
        private val chatRepository: ChatRepository,
        private val apiService: OpenUIApiService,
        private val model: String = "gpt-4o-mini"
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadWelcome()
    }

    private fun loadWelcome() {
        viewModelScope.launch {
            val bffOk = apiService.checkHealth()
            val models = if (bffOk) apiService.fetchModels() else null

            val greeting =
                    if (bffOk && models != null) {
                        "**Welcome to Raiffeisen Bank Kosovo** 🏦\n\nAI assistant is ready. What would you like to do today?"
                    } else {
                        "**Welcome to Raiffeisen Bank Kosovo** 🏦\n\n⚠️ Backend unreachable — check your connection.\n\nBFF (`${apiService.baseUrl}`)  →  ${if (bffOk) "✓ reachable" else "✗ unreachable"}"
                    }

            val actions =
                    if (bffOk && models != null)
                            listOf(
                                    QuickAction(
                                            "💰 Check balance",
                                            "Create a mobile banking account balance overview screen showing total balance EUR 12,450.80, available balance EUR 11,200.00, and a savings account EUR 3,500.00 in a clean card layout with Raiffeisen yellow and black branding"
                                    ),
                                    QuickAction(
                                            "📋 Transactions",
                                            "Create a recent transactions list for a mobile banking app showing 8 realistic transactions with merchant names, dates, and debit/credit amounts in EUR for a Kosovo bank customer"
                                    ),
                                    QuickAction(
                                            "💸 Transfer money",
                                            "Create a money transfer form for a mobile banking app with fields for recipient IBAN, recipient name, amount in EUR, description, and a prominent Transfer button"
                                    ),
                                    QuickAction(
                                            "🧾 Pay a bill",
                                            "Create a utility bill payment screen for a mobile banking app with a dropdown for provider (KESCO electricity, Post of Kosovo, Telecom Kosovo), account number field, amount, and a Pay Bill button"
                                    ),
                                    QuickAction(
                                            "🗺️ Find a branch",
                                            "Create a branch locator for Raiffeisen Bank Kosovo. Show a stylized SVG outline map of Kosovo with colored marker dots for branch locations in Pristina (3 branches), Prizren, Peja, Gjakova, Ferizaj, Mitrovica, and Gjilan. Below the map show a scrollable list of branches with full street address and opening hours Monday-Friday 08:00-17:00, Saturday 09:00-13:00. Use Raiffeisen yellow (#FFCC00) accent color."
                                    )
                            )
                    else emptyList()

            _state.update {
                it.copy(messages = listOf(assistantMessage(greeting)), quickActions = actions)
            }
        }
    }

    fun sendMessage(content: String) {
        if (_state.value.isStreaming || content.isBlank()) return

        val userMsg = userMessage(content)
        val streamingPlaceholder = assistantMessage("", isStreaming = true)

        _state.update {
            it.copy(
                    messages = it.messages + userMsg + streamingPlaceholder,
                    isStreaming = true,
                    error = null,
                    quickActions = emptyList()
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
                    updated[updated.lastIndex] =
                            updated[updated.lastIndex].copy(isStreaming = false)
                    state.copy(messages = updated, isStreaming = false)
                }
            }
                    .onFailure { e ->
                        _state.update {
                            it.copy(isStreaming = false, error = e.message ?: "Streaming failed")
                        }
                    }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun resetChat() {
        _state.update { ChatState() }
        loadWelcome()
    }
}
