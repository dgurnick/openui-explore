package com.dgurnick.openuiexplore.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }
                )
            }
            install(Logging) { level = LogLevel.INFO }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                // No socket/request timeout — SSE streams can run indefinitely
                socketTimeoutMillis = Long.MAX_VALUE
                requestTimeoutMillis = Long.MAX_VALUE
            }
            engine {
                config {
                    readTimeout(0, TimeUnit.MILLISECONDS) // 0 = infinite, required for SSE
                    connectTimeout(10, TimeUnit.SECONDS)
                }
            }
        }
