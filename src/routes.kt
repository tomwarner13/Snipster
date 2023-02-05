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
import kotlinx.coroutines.async
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
        return if (appConfig.envType == EnvType.Local) "$title (LOCAL)" else title
    }

    suspend fun getSnipsForUser(di: LazyDI, username: String?) : Map<Int, SnipDc> {
        if(username == null) return emptyMap()

        val repository by di.instance<SnipRepository>()
        return username.let { u -> repository.getSnipsByUser(u).associate { it.id.value to it.toDc() } }
    }

    suspend fun getSettingsForUser(di: LazyDI, username: String?) : UserSettingsDc {
        if(username == null) return UserSettingsDc("Guest")

        val repo by di.instance<UserSettingsRepository>()
        return repo.getUserSettings(username)
    }

    get("/") {
        val dbCalls = async {
            val snipTask = async { getSnipsForUser(di, call.session?.username) }
            val settingsTask = async { getSettingsForUser(di, call.session?.username) }

            val snipResult = snipTask.await()
            val settingsResult = settingsTask.await()

            return@async Pair(snipResult, settingsResult)
        }

        val (snips, settings) = dbCalls.await()


        call.respondHtmlTemplate(PageTemplate(buildPageHeader("Snipster"), call.session?.username)) {
            headerContent {
                editorSpecificHeaders(snips, settings, call.session?.username)
            }
            navBarContent {
                insert(EditorSpecificNavbarTemplate(call.session?.username != null, call.session?.displayName)) {}
            }
            pageContent {
                insert(Editor(snips, appConfig, settings, call.session?.username)) {}
            }
        }
    }

    get("/about") {
        val settings = if (call.session?.username == null) null else getSettingsForUser(di, call.session?.username)

        call.respondHtmlTemplate(PageTemplate(buildPageHeader("About Snipster"), call.session?.username)) {
            pageContent {
                insert(About(settings)) {}
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

    put("/settings") {
        val username = checkUsername(call)
        val settings = call.receive<UserSettingsDc>()
        if(settings.username != username) throw SecurityException("Cannot modify settings for another user")
        val result = settingsRepo.saveUserSettings(settings)
        call.respond(HttpStatusCode.Accepted, result)
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
        val userSnips = snipRepo.getOwnedSnips(username)
        val session = SnipUserSession(username, userSnips, this, this@setupRoutes)
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

