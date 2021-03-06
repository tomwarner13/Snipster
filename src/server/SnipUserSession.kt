package com.okta.demo.ktor.server

import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.slf4j.Logger
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.*

//TODO track list of open snips per user session here
class SnipUserSession(val username: String, val id: Int, private val session: WebSocketServerSession, private val application: Application)
    : PropertyChangeListener {
    private val log by di{application}.instance<Logger>()
    val sessionId = UUID.randomUUID().toString()

    //TODO generate GUID for session, track on snip edit so that we don't accidentally apply old session edits?

    override fun propertyChange(evt: PropertyChangeEvent?) {
        evt?.newValue?.let {
            if(matchPropertyName(evt.propertyName)) {
                log.debug("detected changed snip: ${evt.propertyName}")
                GlobalScope.launch {
                    val gson = Gson()
                    val frame = Frame.Text(gson.toJson(evt.newValue))
                    session.send(frame)
                    log.debug("sent frame: ${frame.readText()}") //TODO delete as soon as confirmed working
                }//todo can we make sure here that it is a SnipDc?
            }
        }
    }

    private fun matchPropertyName(propertyName: String) : Boolean {
        val values = propertyName.split(':')
        if(values.size != 3) throw IllegalArgumentException("property name '$propertyName' was not in a correct format!")
        //confirm that username and snip ID match, don't match session ID because we don't want to feed changes back to the same session
        return values[0] == username && values[1] == id.toString() && values[2] != sessionId
    }
}