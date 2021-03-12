package com.okta.demo.ktor.server

import com.google.gson.Gson
import com.okta.demo.ktor.database.SnipRepository
import com.okta.demo.ktor.schema.SnipDc
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
class UserConnection(val username: String, private val session: WebSocketServerSession, private val application: Application)
    : PropertyChangeListener {



    private val log by di{application}.instance<Logger>()
    private val _repo by di{application}.instance<SnipRepository>()

    val sessionId = UUID.randomUUID().toString()

    private val userSnips : List<Int> = _repo.getOwnedSnips(username) //what if this changes? notify through property listeners? or always check cache?
    private val otherSnips : List<Int> = emptyList()

    override fun propertyChange(evt: PropertyChangeEvent?) {
        evt?.newValue?.let {
            //listener here also for created snip? deleted? (eventually)
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
        val id = values[2].toInt()
        //confirm that username and snip ID match, don't match session ID because we don't want to feed changes back to the same session
        if(sessionId == values[2]) return false;
        return values[0] == username || otherSnips.any { it == id }
    }

    fun snips() = userSnips + otherSnips
}