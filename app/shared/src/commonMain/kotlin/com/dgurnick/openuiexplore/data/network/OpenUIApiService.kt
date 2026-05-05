package com.dgurnick.openuiexplore.data.network

import com.dgurnick.openuiexplore.data.model.ApiChatMessage
import com.dgurnick.openuiexplore.data.model.ChatCompletionRequest
import com.dgurnick.openuiexplore.data.model.CompletionChunk
import com.dgurnick.openuiexplore.data.model.LoginRequest
import com.dgurnick.openuiexplore.data.model.LoginResponse
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

class OpenUIApiService(
    private val client: HttpClient,
    val baseUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Holds the JWT token after a successful login. Set by AuthRepository.
    var authToken: String? = null

    suspend fun checkHealth(): Boolean = runCatching {
        val response = client.get("$baseUrl/v1/health")
        response.status.isSuccess()
    }.getOrElse { false }

    suspend fun login(username: String, password: String): String {
        val response = client.post("$baseUrl/auth/token") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        return response.body<LoginResponse>().token
    }

    fun streamChat(
        messages: List<ApiChatMessage>,
        model: String = "gpt-4o-mini"
    ): Flow<String> = channelFlow {
        val request = ChatCompletionRequest(
            model = model,
            messages = messages,
            stream = true
        )
        client.preparePost("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            authToken?.let { token -> header(HttpHeaders.Authorization, "Bearer $token") }
            setBody(request)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") return@execute
                    runCatching {
                        val chunk = json.decodeFromString<CompletionChunk>(data)
                        chunk.choices.firstOrNull()?.delta?.content?.let { send(it) }
                    }
                }
            }
        }
    }
}
