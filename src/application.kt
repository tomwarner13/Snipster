package snipster

import snipster.cache.CacheProvider
import snipster.cache.MemoryCacheProvider
import snipster.config.AppConfig
import snipster.config.EnvType
import snipster.database.ConnectionSettings
import snipster.database.DatabaseConnection
import snipster.database.SnipRepository
import snipster.database.UserSettingsRepository
import snipster.schema.UserSession
import snipster.server.SnipServer
import snipster.views.NotFound
import snipster.views.PageTemplate
import snipster.views.ServerError
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
import io.ktor.websocket.*
import org.kodein.di.bind
import org.kodein.di.ktor.di
import org.kodein.di.singleton
import org.slf4j.Logger
import org.slf4j.event.Level
import java.time.Duration
import kotlin.collections.set

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    val appConfig = AppConfig.from(ConfigFactory.load() ?: throw Exception("Failed to load application config"))

    // We use sessions stored in signed cookies
    install(Sessions) {
        cookie<UserSession>("MY_SESSION") {
            val secretEncryptKey = hex(appConfig.sessionEncryptionConfig.encryptionKey)
            val secretAuthKey = hex(appConfig.sessionEncryptionConfig.authKey)
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
            call.respondHtmlTemplate(PageTemplate( "Page Not Found", call.session?.username)) {
                pageContent {
                    insert(NotFound(call.request.path())) {}
                }
            }
        }
        status(HttpStatusCode.InternalServerError) {
            call.respondHtmlTemplate(PageTemplate("Server Error", call.session?.username)) {
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
        //needs to be under the 55 second Heroku request timeout period to keep the connection alive
        pingPeriod = Duration.ofSeconds(30)
    }

    val settings = ConnectionSettings(
        appConfig.databaseConfig.url,
        appConfig.databaseConfig.driver
    )
    val conn = DatabaseConnection(settings)

    di {
        bind<DatabaseConnection>() with singleton { conn }
        bind<SnipRepository>() with singleton { SnipRepository(this@module) }
        bind<UserSettingsRepository>() with singleton { UserSettingsRepository(this@module) }
        bind<Logger>() with singleton { this@module.log }
        bind<SnipServer>() with singleton { SnipServer() }
        bind<CacheProvider>() with singleton { MemoryCacheProvider() }
        bind<AppConfig>() with singleton { appConfig }
    }

    // Configure ktor to use OAuth and register relevant routes
    setupAuth()

    // Register application routes
    setupRoutes()
}

// Shortcut for the current session
val ApplicationCall.session: UserSession?
    get() = sessions.get<UserSession>()