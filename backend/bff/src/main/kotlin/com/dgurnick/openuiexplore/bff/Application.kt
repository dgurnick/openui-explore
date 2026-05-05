package com.dgurnick.openuiexplore.bff

import com.dgurnick.openuiexplore.bff.db.DatabaseFactory
import com.dgurnick.openuiexplore.bff.plugins.configureAuthentication
import com.dgurnick.openuiexplore.bff.plugins.configureDatabase
import com.dgurnick.openuiexplore.bff.plugins.configureLogging
import com.dgurnick.openuiexplore.bff.plugins.configureRouting
import com.dgurnick.openuiexplore.bff.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.launch

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val secret = environment.config.property("jwt.secret").getString()
    if (secret == "dev-secret-change-in-production") {
        log.warn("JWT_SECRET is not set - using insecure default. Set JWT_SECRET in production.")
    }

    val adminPassword = System.getenv("BFF_ADMIN_PASSWORD")
    if (adminPassword == null) {
        log.warn("BFF_ADMIN_PASSWORD is not set - using default 'changeme'. Set it in production.")
    }

    configureDatabase()
    configureLogging()
    configureSerialization()
    configureAuthentication()
    configureRouting()

    launch {
        DatabaseFactory.seedDefaultUser()
    }
}
