package com.dgurnick.openuiexplore.bff.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dgurnick.openuiexplore.bff.db.tables.Users
import com.dgurnick.openuiexplore.bff.model.AuthRequest
import com.dgurnick.openuiexplore.bff.model.ErrorResponse
import com.dgurnick.openuiexplore.bff.model.TokenResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Date

fun Route.authRoutes() {
    post("/auth/token") {
        val request = runCatching { call.receive<AuthRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
            return@post
        }

        val user = newSuspendedTransaction {
            Users.selectAll().where { Users.username eq request.username }.singleOrNull()
        }

        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            return@post
        }

        val isValid = BCrypt.verifyer()
            .verify(request.password.toCharArray(), user[Users.passwordHash])
            .verified

        if (!isValid) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            return@post
        }

        val config = call.application.environment.config
        val secret = config.property("jwt.secret").getString()
        val issuer = config.property("jwt.issuer").getString()
        val audience = config.property("jwt.audience").getString()
        val expirationHours = config.propertyOrNull("jwt.expirationHours")?.getString()?.toLong() ?: 24L

        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("username", request.username)
            .withExpiresAt(Date(System.currentTimeMillis() + expirationHours * 3_600_000L))
            .sign(Algorithm.HMAC256(secret))

        call.respond(TokenResponse(token))
    }
}
