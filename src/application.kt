package com.okta.demo.ktor

import com.okta.demo.ktor.database.ConnectionSettings
import com.okta.demo.ktor.database.DatabaseConnection
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.server.SnipServer
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.sessions.*
import io.ktor.util.*
import org.kodein.di.bind
import org.slf4j.event.Level
import kotlin.collections.set
import org.kodein.di.ktor.di
import org.kodein.di.singleton
import io.ktor.websocket.*
import java.time.Duration
import org.slf4j.Logger

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    // We use sessions stored in signed cookies
    install(Sessions) {
        cookie<UserSession>("MY_SESSION") {
            val secretEncryptKey = hex("00112233445566778899aabbccddeeff")
            val secretAuthKey = hex("02030405060708090a0b0c")
            cookie.extensions["SameSite"] = "lax"
            cookie.httpOnly = true
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretAuthKey))
        }
    }

    // Respond for HEAD verb
    install(AutoHeadResponse)

    // Load each request
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    //required to return JSON
    install(ContentNegotiation) {
        gson()
    }

    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
    }

    // Configure ktor to use OAuth and register relevant routes
    setupAuth()

    // Register application routes
    setupRoutes()

    val conn = setupDatabase()
    //can put all DI registrations in here
    di {
        bind<DatabaseConnection>() with singleton { conn }
        bind<SnipRepository>() with singleton { SnipRepository(this@module) }
        bind<Logger>() with singleton { this@module.log }
        bind<SnipServer>() with singleton { SnipServer() }
    }
}


// Shortcut for the current session
val ApplicationCall.session: UserSession?
    get() = sessions.get<UserSession>()

fun setupDatabase() : DatabaseConnection {
    val config = ConfigFactory.load()
    val settings = ConnectionSettings(
        config.getString("database.url"),
        config.getString("database.driver"),
        config.getString("database.user"),
        config.getString("database.password"))

    return DatabaseConnection(settings)
}

