package com.dgurnick.openuiexplore.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiChatMessage>,
    val stream: Boolean = true
)

@Serializable
data class ApiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class CompletionChunk(
    val choices: List<ChunkChoice> = emptyList()
)

@Serializable
data class ChunkChoice(
    val delta: ChunkDelta,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChunkDelta(
    val content: String? = null,
    val role: String? = null
)
