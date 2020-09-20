package com.example.db

import com.example.db.Database.Companion.Schema
import com.example.db.Database.Companion.invoke
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import timber.log.Timber
import timber.log.debug
import java.util.*

/**
 * Database helper inits driver, creates schema, handles migrations and closes database.
 *
 * @param filePath path to database file or empty string for in-memory database
 *
 * @property foreignKeys Corresponds to foreign_keys pragma and is true by default.
 * It turns on after schema creation for new databases, but turns before migration for existent databases.
 *
 * @property journalMode Corresponds to journal_mode pragma, should be set before first access to [database] property.
 */
class DbHelper(
    val filePath: String = "",
    journalMode: String = "",
    foreignKeys: Boolean = false
) {
    val database: Database by lazy {
        if (journalMode.isNotEmpty()) {
            this.journalMode = journalMode
        }
        val currentVer = version
        if (currentVer == 0) {
            Schema.create(driver)
            version = Schema.version
            Timber.debug { "init: created tables, setVersion to 1" }
            this.foreignKeys = foreignKeys
        } else {
            this.foreignKeys = foreignKeys
            val schemaVer = Schema.version
            if (schemaVer > currentVer) {
                Schema.migrate(driver, currentVer, schemaVer)
                version = schemaVer
                this.foreignKeys = foreignKeys
                Timber.debug { "init: migrated from $currentVer to $schemaVer" }
            } else {
                Timber.debug { "init" }
            }
        }
        invoke(driver)
    }

    fun close() {
        driver.close()
    }

    private val driver: JdbcSqliteDriver by lazy {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + filePath, Properties())
    }

    private var version: Int
        get() {
            val sqlCursor = driver.executeQuery(null, "PRAGMA user_version;", 0, null)
            return sqlCursor.getLong(0)!!.toInt()
        }
        set(value) {
            driver.execute(null, "PRAGMA user_version = $value;", 0, null)
        }

    var journalMode: String = journalMode
        private set(value) {
            driver.execute(null, "PRAGMA journal_mode = $value;", 0, null)
            field = value
        }

    var foreignKeys: Boolean = foreignKeys
        set(value) {
            driver.execute(null, "PRAGMA foreign_keys = ${if (value) 1 else 0};", 0, null)
            field = value
        }
}
