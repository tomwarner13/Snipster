package snipster.database

import io.ktor.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.slf4j.Logger
import snipster.server.SnipServer

abstract class DatabaseBase(private val application: Application) {
    protected val _conn by di{application}.instance<DatabaseConnection>()
    protected val _server by di{application}.instance<SnipServer>()
    protected val _db : Database = _conn.db
    protected val _logger by di{application}.instance<Logger>()

    suspend fun <T> dbExec(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction { block() }
    }

    suspend fun dbUpdate(block: () -> Unit ) = withContext(Dispatchers.IO) {
        transaction { block() }
    }
}