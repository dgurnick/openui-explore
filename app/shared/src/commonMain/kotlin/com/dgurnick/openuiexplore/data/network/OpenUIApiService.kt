package com.dgurnick.openuiexplore.data.network

import com.dgurnick.openuiexplore.data.model.ApiChatMessage
import com.dgurnick.openuiexplore.data.model.ChatCompletionRequest
import com.dgurnick.openuiexplore.data.model.CompletionChunk
import com.dgurnick.openuiexplore.data.model.LoginRequest
import com.dgurnick.openuiexplore.data.model.LoginResponse
import com.dgurnick.openuiexplore.data.model.ModelsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json

class OpenUIApiService(private val client: HttpClient, val baseUrl: String) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Holds the JWT token after a successful login. Set by AuthRepository.
    var authToken: String? = null

    companion object {
        // Mirrors the system prompt used by the OpenUI frontend (frontend/src/api/openai.ts)
        val SYSTEM_PROMPT =
                """
🎉 Greetings, TailwindCSS Virtuoso! 🌟

You've mastered the art of frontend design and TailwindCSS! Your mission is to transform detailed descriptions or compelling images into stunning HTML using the versatility of TailwindCSS. Ensure your creations are seamless in both dark and light modes! Your designs should be responsive and adaptable across all devices – be it desktop, tablet, or mobile.

*Design Guidelines:*
- Utilize placehold.co for placeholder images and descriptive alt text.
- For interactive elements, leverage modern ES6 JavaScript and native browser APIs for enhanced functionality.

*Implementation Rules:*
- Only implement elements within the `<body>` tag, don't bother with `<html>` or `<head>` tags.
- Avoid using SVGs directly. Instead, use the `<img>` tag with a descriptive title as the alt attribute.

Always start your response with frontmatter wrapped in ---.  Set name: with a 2 to 5 word description of the component. Set emoji: with an emoji for the component, i.e.:
---
name: Fancy Button
emoji: 🎉
---

<button class="bg-blue-500 text-white p-2 rounded-lg">Click me</button>
        """.trimIndent()
    }

    suspend fun checkHealth(): Boolean =
            runCatching {
                val response = client.get("$baseUrl/v1/health")
                response.status.isSuccess()
            }
                    .getOrElse { false }

    suspend fun fetchModels(): ModelsResponse =
            runCatching {
                val response =
                        client.get("$baseUrl/v1/models") {
                            authToken?.let { token ->
                                header(HttpHeaders.Authorization, "Bearer $token")
                            }
                        }
                response.body<ModelsResponse>()
            }
                    .getOrElse { ModelsResponse() }

    suspend fun login(username: String, password: String): String {
        val response =
                client.post("$baseUrl/auth/token") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(username, password))
                }
        return response.body<LoginResponse>().token
    }

    fun streamChat(messages: List<ApiChatMessage>, model: String = "gpt-4o-mini"): Flow<String> =
            channelFlow {
                val systemMessage = ApiChatMessage(role = "system", content = SYSTEM_PROMPT)
                val request =
                        ChatCompletionRequest(
                                model = model,
                                messages = listOf(systemMessage) + messages,
                                stream = true
                        )
                client
                        .preparePost("$baseUrl/v1/chat/completions") {
                            contentType(ContentType.Application.Json)
                            authToken?.let { token ->
                                header(HttpHeaders.Authorization, "Bearer $token")
                            }
                            setBody(request)
                        }
                        .execute { response ->
                            val channel = response.bodyAsChannel()
                            while (!channel.isClosedForRead) {
                                val line = channel.readUTF8Line() ?: break
                                if (line.startsWith("data: ")) {
                                    val data = line.removePrefix("data: ").trim()
                                    if (data == "[DONE]") return@execute
                                    runCatching {
                                        val chunk = json.decodeFromString<CompletionChunk>(data)
                                        chunk.choices.firstOrNull()?.delta?.content?.let {
                                            send(it)
                                        }
                                    }
                                }
                            }
                        }
            }
}
