package com.okta.demo.ktor

import com.google.gson.Gson
import com.okta.demo.ktor.config.AppConfig
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.schema.SnipDc
import com.okta.demo.ktor.server.SnipServer
import com.okta.demo.ktor.server.SnipUserSession
import com.okta.demo.ktor.views.*
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
import org.kodein.di.instance
import org.kodein.di.ktor.di

fun Application.setupRoutes() = routing {
    val di = di()
    val repo by di.instance<SnipRepository>()
    val server by di.instance<SnipServer>()
    val appConfig by di.instance<AppConfig>()

    fun checkUsername(call: ApplicationCall) : String {
        return call.session?.username ?: throw SecurityException("must be logged in")
    }

    get("/") {
        val snips = Editor.getSnipsForUser(di, call.session?.username)

        call.respondHtmlTemplate(PageTemplate(appConfig, "Snipster", call.session?.username, call.session?.displayName)) {
            headerContent {
                editorSpecificHeaders(snips, call.session?.username)
            }
            pageContent {
                insert(Editor(snips, call.session?.username)) {}
            }
        }
    }

    get("/jar") {
        val snips = CodeJarEditor.getSnipsForUser(di, call.session?.username)

        call.respondHtmlTemplate(PageTemplate(appConfig, "Snipster", call.session?.username, call.session?.displayName)) {
            headerContent {
                codeJareditorSpecificHeaders(snips, call.session?.username)
            }
            pageContent {
                insert(CodeJarEditor(snips, call.session?.username)) {}
            }
        }
    }

    get("/about") {
        call.respondHtmlTemplate(PageTemplate(appConfig, "About Snipster", call.session?.username, call.session?.displayName)) {
            pageContent {
                insert(About()) {}
            }
        }
    }

    //404, 5xx generic pages, user settings page? dark mode with cool hacker text?

    get("/snips") {
        val username = checkUsername(call)
        log.debug("$username requesting all snips")
        val result = repo.getSnipsByUser(username).map { it.toDc() } //fix DB call to create if none exists?
        log.debug(result.toString())
        call.respond(HttpStatusCode.Found, result)
    }

    post("/snips") {
        val username = checkUsername(call)
        val snip = call.receive<SnipDc>()
        log.debug("$username creating snip:")
        log.debug(snip.toString())
        val result = repo.createSnip(username, snip.title, snip.content).toDc()
        call.respond(HttpStatusCode.Created, result)
    }

    put("/snips/{id}") {
        val username = checkUsername(call)
        val snip = call.receive<SnipDc>() //do i want this? or just the fields used? will it work by just giving fields used?
        log.debug("$username editing snip:")
        log.debug(snip.toString())
        repo.editSnip(snip)
        call.respond(HttpStatusCode.NoContent)
    }

    delete("/snips/{id}") {
        val username = checkUsername(call)
        val id = call.parameters["id"]?.toInt() ?: throw SecurityException("snip ID required for deletion")
        repo.deleteSnip(id, username)
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
                    repo.editSnip(snip)
                }
            }
        }
        catch(e: Exception) {
            log.error("websocket error! $e")
            throw e //TODO literally any handling here
        }
        finally {
            server.removeSession(session)
        }
    }

    static {
        resources("web") //serve files from resources/web
    }
}

