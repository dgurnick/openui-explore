package com.dgurnick.openuiexplore.data.model

import kotlin.random.Random

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false
)

fun userMessage(content: String) = ChatMessage(
    id = Random.nextLong().toString(16),
    role = Role.USER,
    content = content
)

fun assistantMessage(content: String, isStreaming: Boolean = false) = ChatMessage(
    id = Random.nextLong().toString(16),
    role = Role.ASSISTANT,
    content = content,
    isStreaming = isStreaming
)
