package com.dgurnick.openuiexplore.bff.routes

import com.dgurnick.openuiexplore.bff.db.tables.RequestLogs
import com.dgurnick.openuiexplore.bff.db.tables.Users
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.copyTo
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl as OkHttpUrl
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Route.proxyRoutes(openUIBaseUrl: String) {
    val cookieJar =
            object : CookieJar {
                private val store = mutableListOf<Cookie>()
                override fun saveFromResponse(url: OkHttpUrl, cookies: List<Cookie>) {
                    synchronized(store) {
                        store.removeAll { existing -> cookies.any { it.name == existing.name } }
                        store.addAll(cookies)
                    }
                }
                override fun loadForRequest(url: OkHttpUrl): List<Cookie> =
                        synchronized(store) { store.toList() }
            }

    val client =
            HttpClient(OkHttp) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(io.ktor.client.plugins.HttpTimeout) {
                    connectTimeoutMillis = 10_000
                    // Infinite timeouts for SSE streaming — LLM responses can be slow
                    socketTimeoutMillis = Long.MAX_VALUE
                    requestTimeoutMillis = Long.MAX_VALUE
                }
                // Disable automatic redirects so SSE streams are not interrupted
                engine {
                    config {
                        followRedirects(false)
                        cookieJar(cookieJar)
                        readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS) // 0 = infinite
                        connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    }
                }
            }

    // Health check - no auth required
    get("/v1/health") {
        val ok =
                runCatching {
                    val response = client.get("$openUIBaseUrl/v1/health")
                    response.status == HttpStatusCode.OK
                }
                        .getOrElse { false }

        if (ok) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

    authenticate("jwt-auth") {
        // Model list
        get("/v1/models") {
            val start = System.currentTimeMillis()
            val response = client.get("$openUIBaseUrl/v1/models")
            val body = response.bodyAsText()
            logRequest(
                    call.principal<JWTPrincipal>(),
                    "GET",
                    call.request.path(),
                    response.status.value,
                    start
            )
            call.respondText(body, ContentType.Application.Json, response.status)
        }

        // Chat completions - streams SSE tokens verbatim from OpenUI to the client
        post("/v1/chat/completions") {
            // Ensure the BFF has an active session with the OpenUI backend.
            // In LOCAL mode /v1/session auto-creates a user session; the cookie
            // is persisted in the cookieJar and forwarded on subsequent requests.
            runCatching { client.get("$openUIBaseUrl/v1/session") }

            val body = call.receiveText()
            val start = System.currentTimeMillis()

            // Debug: log first 300 chars of request body to verify system message is present
            call.application.environment.log.info("CHAT BODY: ${body.take(300).replace("\n", " ")}")

            client
                    .preparePost("$openUIBaseUrl/v1/chat/completions") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                    .execute { upstream ->
                        val contentType =
                                upstream.headers[HttpHeaders.ContentType]
                                        ?: ContentType.Application.Json.toString()

                        call.response.header(HttpHeaders.ContentType, contentType)
                        call.response.header(HttpHeaders.CacheControl, "no-cache")

                        call.respondBytesWriter(status = upstream.status) {
                            upstream.bodyAsChannel().copyTo(this)
                        }

                        logRequest(
                                call.principal<JWTPrincipal>(),
                                "POST",
                                call.request.path(),
                                upstream.status.value,
                                start
                        )
                    }
        }
    }
}

private suspend fun logRequest(
        principal: JWTPrincipal?,
        method: String,
        path: String,
        statusCode: Int,
        startMs: Long
) {
    val username = principal?.payload?.getClaim("username")?.asString()
    val durationMs = System.currentTimeMillis() - startMs

    val userId =
            if (username != null) {
                runCatching {
                            newSuspendedTransaction {
                                Users.selectAll()
                                        .where { Users.username eq username }
                                        .singleOrNull()
                                        ?.get(Users.id)
                                        ?.value
                            }
                        }
                        .getOrNull()
            } else null

    runCatching {
        newSuspendedTransaction {
            RequestLogs.insert {
                it[RequestLogs.userId] = userId
                it[RequestLogs.method] = method
                it[RequestLogs.path] = path
                it[RequestLogs.statusCode] = statusCode
                it[RequestLogs.durationMs] = durationMs
            }
        }
    }
}
