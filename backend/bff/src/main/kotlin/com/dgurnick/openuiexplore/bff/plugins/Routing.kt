package com.dgurnick.openuiexplore.bff.plugins

import com.dgurnick.openuiexplore.bff.model.ErrorResponse
import com.dgurnick.openuiexplore.bff.routes.authRoutes
import com.dgurnick.openuiexplore.bff.routes.proxyRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.dgurnick.openuiexplore.bff.routing")

fun Application.configureRouting() {
    val openUIBaseUrl = environment.config.property("openui.backendUrl").getString()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception on ${call.request.local.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal server error")
            )
        }
    }

    routing {
        authRoutes()
        proxyRoutes(openUIBaseUrl)
    }
}
