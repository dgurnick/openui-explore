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
import io.ktor.server.request.httpMethod
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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun Route.proxyRoutes(openUIBaseUrl: String) {
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // Disable automatic redirects so SSE streams are not interrupted
        engine {
            config {
                followRedirects(false)
            }
        }
    }

    // Health check - no auth required
    get("/v1/health") {
        val ok = runCatching {
            val response = client.get("$openUIBaseUrl/v1/health")
            response.status == HttpStatusCode.OK
        }.getOrElse { false }

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
            logRequest(call.principal<JWTPrincipal>(), "GET", call.request.path(), response.status.value, start)
            call.respondText(body, ContentType.Application.Json, response.status)
        }

        // Chat completions - streams SSE tokens verbatim from OpenUI to the client
        post("/v1/chat/completions") {
            val body = call.receiveText()
            val start = System.currentTimeMillis()

            client.preparePost("$openUIBaseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.execute { upstream ->
                val contentType = upstream.headers[HttpHeaders.ContentType]
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

    val userId = if (username != null) {
        runCatching {
            newSuspendedTransaction {
                Users.selectAll().where { Users.username eq username }
                    .singleOrNull()?.get(Users.id)?.value
            }
        }.getOrNull()
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
