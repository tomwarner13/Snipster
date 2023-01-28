package com.okta.demo.ktor.database

import com.okta.demo.ktor.cache.CacheProvider
import com.okta.demo.ktor.schema.UserSettings
import com.okta.demo.ktor.schema.UserSettingsDc
import com.okta.demo.ktor.schema.UserSettingsTable
import com.okta.demo.ktor.server.SnipServer
import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.slf4j.Logger

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
}