package snipster.database

import org.jetbrains.exposed.sql.Database

//idea here is to just have a connection available for other classes to use
class DatabaseConnection(private val settings: ConnectionSettings) {
    val db : Database by lazy {
        Database.connect(settings.url, driver = settings.driver)
    }
}