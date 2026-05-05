package com.dgurnick.openuiexplore.bff.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object RequestLogs : LongIdTable("request_logs") {
    val userId = long("user_id").nullable()
    val method = varchar("method", 10)
    val path = varchar("path", 512)
    val statusCode = integer("status_code")
    val durationMs = long("duration_ms")
    // created_at is managed by SQLite DEFAULT (datetime('now')) via Flyway migration
}
