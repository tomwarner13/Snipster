package com.okta.demo.ktor

import com.okta.demo.ktor.cache.CacheProvider
import com.okta.demo.ktor.cache.MemoryCacheProvider
import com.okta.demo.ktor.database.ConnectionSettings
import com.okta.demo.ktor.database.DatabaseConnection
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.server.SnipServer
import com.okta.demo.ktor.views.Editor
import com.okta.demo.ktor.views.NotFound
import com.okta.demo.ktor.views.PageTemplate
import com.okta.demo.ktor.views.ServerError
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.html.*
import io.ktor.http.*
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
            val secretEncryptKey = hex("00112233445566778899aabbccddeeff") //TODO this should probably not be in source control wtf
            val secretAuthKey = hex("02030405060708090a0b0c")
            cookie.extensions["SameSite"] = "lax"
            cookie.httpOnly = true
            transform(SessionTransportTransformerEncrypt(secretEncryptKey, secretAuthKey))
        }
    }

    // Respond for HEAD verb
    install(AutoHeadResponse)

    //automatic responses on error status
    install(StatusPages) {
        status(HttpStatusCode.NotFound) {
            call.respondHtmlTemplate(PageTemplate("Page Not Found", call.session?.username, call.session?.displayName)) {
                pageContent {
                    insert(NotFound(call.request.path())) {}
                }
            }
        }
        status(HttpStatusCode.InternalServerError) {
            call.respondHtmlTemplate(PageTemplate("Server Error", call.session?.username, call.session?.displayName)) {
                val status = call.response.status() ?: HttpStatusCode.InternalServerError
                pageContent {
                    insert(ServerError(call.request.path(), status)) {}
                }
            }
        }
    }

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
        bind<CacheProvider>() with singleton { MemoryCacheProvider() }
    }
}


// Shortcut for the current session
val ApplicationCall.session: UserSession?
    get() = sessions.get<UserSession>()

fun setupDatabase() : DatabaseConnection {
    val config = ConfigFactory.load()
    val settings = ConnectionSettings(
        config.getString("database.url"),
        config.getString("database.driver")
    )

    return DatabaseConnection(settings)
}

