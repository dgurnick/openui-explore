package com.dgurnick.openuiexplore.bff.plugins

import com.dgurnick.openuiexplore.bff.db.DatabaseFactory
import io.ktor.server.application.Application

fun Application.configureDatabase() {
    val dbPath = environment.config.propertyOrNull("database.path")?.getString()
        ?: "./openui-bff.db"
    DatabaseFactory.init(dbPath)
}
