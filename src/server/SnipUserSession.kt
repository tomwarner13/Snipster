package snipster.server

import com.google.gson.Gson
import snipster.database.SnipRepository
import snipster.helper.SnipChangeEvent
import snipster.schema.ChangeType
import snipster.schema.ClientMessage
import snipster.schema.SnipDc
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
class SnipUserSession(val username: String, private val session: WebSocketServerSession, private val application: Application)
    : PropertyChangeListener {

    private val log by di{application}.instance<Logger>()
    private val _repo by di{application}.instance<SnipRepository>()

    val sessionId = UUID.randomUUID().toString()

    private val userSnips : MutableSet<Int> = _repo.getOwnedSnips(username) //what if this changes? notify through property listeners? or always check cache?
    private val otherSnips : MutableSet<Int> = emptyList<Int>().toMutableSet()

    override fun propertyChange(evt: PropertyChangeEvent?) {
        evt?.newValue?.let {
            val event = matchPropertyName(evt.propertyName)
            if(event != null && evt.newValue is SnipDc) { //handle deletes without full object?
                val dc = evt.newValue as SnipDc
                val message = ClientMessage(event.type, dc)
                log.debug("detected change: ${evt.propertyName}")

                GlobalScope.launch { //notify client(s)
                    val gson = Gson()
                    val frame = Frame.Text(gson.toJson(message))
                    session.send(frame)
                }

                when(event.type) { //update records
                    ChangeType.Created -> {
                        userSnips.add(event.id)
                    }
                    ChangeType.Deleted -> {
                        userSnips.remove(event.id)
                    }
                }
            }
        }
    }

    private fun matchPropertyName(propertyName: String) : SnipChangeEvent? {
        val event = SnipChangeEvent.fromPropertyName(propertyName)
        if(event.sessionId == sessionId) return null
        if(event.username == username) return event
        if(otherSnips.any {it == event.id}) return event
        return null
    }

    fun snips() = userSnips + otherSnips
}