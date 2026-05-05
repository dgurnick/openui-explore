package com.dgurnick.openuiexplore.bff.db

import at.favre.lib.crypto.bcrypt.BCrypt
import com.dgurnick.openuiexplore.bff.db.tables.RequestLogs
import com.dgurnick.openuiexplore.bff.db.tables.Users
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {

    fun init(dbPath: String) {
        val jdbcUrl = "jdbc:sqlite:$dbPath"

        // Flyway runs SQL migrations from resources/db/migration/
        Flyway.configure()
            .dataSource(jdbcUrl, null, null)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        // Connect Exposed after migrations are applied
        Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")
    }

    // Seeds the initial admin user from env vars if no users exist yet.
    // Called once on startup after the database is initialised.
    suspend fun seedDefaultUser() {
        val adminUsername = System.getenv("BFF_ADMIN_USERNAME") ?: "admin"
        val adminPassword = System.getenv("BFF_ADMIN_PASSWORD") ?: "changeme"

        newSuspendedTransaction {
            val count = Users.selectAll().count()
            if (count == 0L) {
                val hash = BCrypt.withDefaults().hashToString(12, adminPassword.toCharArray())
                Users.insert {
                    it[username] = adminUsername
                    it[passwordHash] = hash
                }
            }
        }
    }

    // Unused by Exposed directly - Flyway owns schema creation.
    // Listed here as documentation of the managed tables.
    @Suppress("unused")
    val managedTables = arrayOf(Users, RequestLogs)
}
