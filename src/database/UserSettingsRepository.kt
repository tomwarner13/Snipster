package com.okta.demo.ktor.database

import com.okta.demo.ktor.schema.UserSettings
import com.okta.demo.ktor.schema.UserSettingsDc
import com.okta.demo.ktor.schema.UserSettingsTable
import com.okta.demo.ktor.server.SnipServer
import io.ktor.application.*
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.slf4j.Logger
import java.sql.PreparedStatement

class UserSettingsRepository(private val application: Application) {
    private val _conn by di{application}.instance<DatabaseConnection>()
    private val _server by di{application}.instance<SnipServer>()
    private val _db : Database = _conn.db
    private val _logger by di{application}.instance<Logger>()

    fun getUserSettings(username: String) : UserSettingsDc {
        return transaction {
            var result = UserSettings.find { UserSettingsTable.username eq username }.firstOrNull()
            if(result != null) {
                return@transaction result.toDc()
            } else {
                return@transaction UserSettingsDc(username)
            }
        }
    }

    fun saveUserSettings(settings: UserSettingsDc) {
        transaction {
            val conn = TransactionManager.current().connection

            val query = """
                INSERT INTO user_settings (username, use_line_numbers, insert_closing) VALUES (?, ?, ?) ON CONFLICT (username) DO UPDATE SET use_line_numbers=?, insert_closing = ?;
            """.trimIndent()
            val statement = conn.prepareStatement(query, false)

            statement.fillParameters(listOf( //WOW IF ONLY THERE WERE A WAY TO NAME AND LABEL PARAMETERS
                Pair(VarCharColumnType(), settings.username),
                Pair(BooleanColumnType(), settings.useLineNumbers),
                Pair(BooleanColumnType(), settings.insertClosing),
                Pair(BooleanColumnType(), settings.useLineNumbers),
                Pair(BooleanColumnType(), settings.insertClosing)
            ))
            statement.executeUpdate()
        }
    }
}