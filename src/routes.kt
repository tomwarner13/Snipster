package snipster

import com.google.gson.Gson
import snipster.config.AppConfig
import snipster.config.EnvType
import snipster.database.SnipRepository
import snipster.database.UserSettingsRepository
import snipster.schema.SnipDc
import snipster.schema.UserSettingsDc
import snipster.server.SnipServer
import snipster.server.SnipUserSession
import snipster.views.*
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import org.kodein.di.LazyDI
import org.kodein.di.instance
import org.kodein.di.ktor.di

fun Application.setupRoutes() = routing {
    val di = di()
    val snipRepo by di.instance<SnipRepository>()
    val settingsRepo by di.instance<UserSettingsRepository>()
    val server by di.instance<SnipServer>()
    val appConfig by di.instance<AppConfig>()

    fun checkUsername(call: ApplicationCall) : String {
        return call.session?.username ?: throw SecurityException("must be logged in")
    }

    fun buildPageHeader(title: String) : String {
        return if (appConfig.envType == EnvType.Local) "$title (LOCAL)" else title;
    }

    fun getSnipsForUser(di: LazyDI, username: String?) : Map<Int, SnipDc> {
        if(username == null) return emptyMap()

        val repository by di.instance<SnipRepository>()
        return username.let { u -> repository.getSnipsByUser(u).map { it.id.value to it.toDc() }.toMap()}
    }

    get("/") {
        val snips = getSnipsForUser(di, call.session?.username)

        call.respondHtmlTemplate(PageTemplate(appConfig, buildPageHeader("Snipster"), call.session?.username, call.session?.displayName)) {
            headerContent {
                editorSpecificHeaders(snips, call.session?.username)
            }
            pageContent {
                insert(Editor(snips, call.session?.username)) {}
            }
        }
    }

    get("/about") {
        call.respondHtmlTemplate(PageTemplate(appConfig, buildPageHeader("About Snipster"), call.session?.username, call.session?.displayName)) {
            pageContent {
                insert(About()) {}
            }
        }
    }

    get("/snips") {
        val username = checkUsername(call)
        val result = snipRepo.getSnipsByUser(username).map { it.toDc() } //fix DB call to create if none exists?
        call.respond(HttpStatusCode.Found, result)
    }

    get("/settings") {
        val username = checkUsername(call)
        val result = settingsRepo.getUserSettings(username)
        call.respond(HttpStatusCode.Found, result)
    }

    get("/settings/update") {
        val username = checkUsername(call)
        var settings = UserSettingsDc(username, true, false)
        val result = settingsRepo.saveUserSettings(settings)
        call.respond(HttpStatusCode.NoContent)
    }

    post("/snips") {
        val username = checkUsername(call)
        val snip = call.receive<SnipDc>()
        log.debug("$username creating snip:")
        log.debug(snip.toString())
        val result = snipRepo.createSnip(username, snip.title, snip.content).toDc()
        call.respond(HttpStatusCode.Created, result)
    }

    put("/snips/{id}") {
        val username = checkUsername(call)
        val snip = call.receive<SnipDc>() //do i want this? or just the fields used? will it work by just giving fields used?
        log.debug("$username editing snip:")
        log.debug(snip.toString())
        snipRepo.editSnip(snip)
        call.respond(HttpStatusCode.NoContent)
    }

    delete("/snips/{id}") {
        val username = checkUsername(call)
        val id = call.parameters["id"]?.toInt() ?: throw SecurityException("snip ID required for deletion")
        snipRepo.deleteSnip(id, username)
        call.respond(HttpStatusCode.NoContent)
    }

    webSocket("/socket/{username}") {
        val username = checkUsername(call)
        if(username != call.parameters["username"]) throw SecurityException("attempted to modify unowned snips!")
        log.debug("$username opening socket")
        val session = SnipUserSession(username, this, this@setupRoutes)
        server.registerSession(session)

        try {
            incoming.consumeEach { frame ->
                // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                if (frame is Frame.Text) {
                    // only worry about text frames for now
                    val gson = Gson()
                    val text = frame.readText()
                    log.debug("text received: $text")
                    val snip = gson.fromJson(text, SnipDc::class.java)
                    snip.editingSessionId = session.sessionId
                    log.debug("updating snip " + snip.id)
                    snipRepo.editSnip(snip)
                }
            }
        }
        catch(e: Exception) {
            log.error("websocket error! $e") //TODO literally any handling here
        }
        finally {
            server.removeSession(session)
        }
    }

    static {
        resources("web") //serve files from resources/web
    }
}

