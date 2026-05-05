package com.dgurnick.openuiexplore.bff.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable("users") {
    val username = varchar("username", 64).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    // created_at is managed by SQLite DEFAULT (datetime('now')) via Flyway migration
}
