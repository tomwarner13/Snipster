package snipster.server

import snipster.helper.SnipChangeEvent
import snipster.schema.ChangeType
import snipster.schema.SnipDc
import java.beans.PropertyChangeSupport

class SnipServer {
    private val support: PropertyChangeSupport = PropertyChangeSupport(this)
    private val oldMessage = "Prior value not tracked! This property should never be called"

    fun registerSession(session: SnipUserSession) {
        support.addPropertyChangeListener(session)
    }

    fun removeSession(session: SnipUserSession) {
        support.removePropertyChangeListener(session)
    }

    fun snipCreated(snip: SnipDc) {
        val event = SnipChangeEvent(
            snip.id,
            ChangeType.Created,
            snip.username,
            snip.editingSessionId
        )
        support.firePropertyChange(event.toPropertyName(), oldMessage, snip)
    }

    fun snipUpdated(snip: SnipDc) {
        val event = SnipChangeEvent(
            snip.id,
            ChangeType.Edited,
            snip.username,
            snip.editingSessionId
        )
        support.firePropertyChange(event.toPropertyName(),
            oldMessage, snip)
    }

    fun snipDeleted(snip: SnipDc) {
        val event = SnipChangeEvent(
            snip.id,
            ChangeType.Deleted,
            snip.username,
            snip.editingSessionId
        )
        support.firePropertyChange(event.toPropertyName(), oldMessage, snip)
    }
}