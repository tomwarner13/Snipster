package snipster.database

import snipster.schema.UserSettings
import snipster.schema.UserSettingsDc
import snipster.schema.UserSettingsTable
import snipster.server.SnipServer
import io.ktor.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.slf4j.Logger
import java.sql.PreparedStatement

class UserSettingsRepository(private val application: Application) : DatabaseBase(application) {
    suspend fun getUserSettings(username: String) : UserSettingsDc {
        return dbExec {
            var result = UserSettings.find { UserSettingsTable.username eq username }.firstOrNull()
            if(result != null) {
                return@dbExec result.toDc()
            } else {
                return@dbExec UserSettingsDc(username)
            }
        }
    }

    suspend fun saveUserSettings(settings: UserSettingsDc) {
        dbUpdate {
            val conn = TransactionManager.current().connection

            val query =
                "INSERT INTO user_settings (username, use_line_numbers, insert_closing) VALUES (?, ?, ?) ON CONFLICT (username) DO UPDATE SET use_line_numbers=?, insert_closing = ?;"
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