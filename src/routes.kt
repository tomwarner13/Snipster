package com.okta.demo.ktor

import com.google.gson.Gson
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.schema.SnipDc
import com.okta.demo.ktor.server.SnipServer
import com.okta.demo.ktor.server.SnipUserSession
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
    val repo by di().instance<SnipRepository>()
    val server by di().instance<SnipServer>()

    fun checkUsername(call: ApplicationCall) : String {
        return call.session?.username ?: throw SecurityException("must be logged in")
    }

    get("/") {
        call.respondHtmlTemplate(ScratchTemplate(call.session?.username, call.session?.displayName)) {
            content {
                textEditor()
            }
        }
    }

    get("/snips") {
        val username = checkUsername(call)
        log.debug("$username requesting all snips")
        val result = repo.getSnipsByUser(username).map { it.toDc() } //fix DB call to create if none exists?
        log.debug(result.toString());
        call.respond(HttpStatusCode.Found, result)
    }

    //TEMP TEXT ROUT TO DELETE
    get("/snips/create") {
        val username = checkUsername(call)
        log.debug("$username creating snip")

        val result = repo.createSnip(username, "test title", "test content").toDc()
        call.respond(HttpStatusCode.Found, result)
    }

    post("/snips") {
        val username = checkUsername(call)
        val snip = call.receive<SnipDc>() //do i want this? or just the fields used? will it work by just giving fields used?
        log.debug("$username creating snip:")
        log.debug(snip.toString())
        repo.createSnip(username, snip.title, snip.content)
    }

    put("/snips/{id}") {
        val username = checkUsername(call)
        val snip = call.receive<SnipDc>() //do i want this? or just the fields used? will it work by just giving fields used?
        log.debug("$username creating snip:")
        log.debug(snip.toString())
        repo.editSnip(snip)
    }

    delete("/snips/{id}") {
        //TODO this
    }

    webSocket("/socket/{id}") { //TODO change this to username and keep persistent track of all snips open by user?
        val username = checkUsername(call)
        val id = call.parameters["id"]?.toInt() ?: throw SecurityException("snip ID must be provided")
        log.debug("$username opening socket for snip $id")
        val session = SnipUserSession(username, id, this, this@setupRoutes)
        server.registerSession(session)

        try {
            incoming.consumeEach { frame ->
                // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                // We are only interested in textual messages, so we filter it.
                if (frame is Frame.Text) {
                    // only worry about text frames for now
                    val gson = Gson()
                    var text = frame.readText()
                    log.debug("text received: $text")
                    var snip = gson.fromJson(text, SnipDc::class.java)
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

