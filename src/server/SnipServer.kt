package com.okta.demo.ktor.server

import com.okta.demo.ktor.schema.SnipDc
import java.beans.PropertyChangeSupport

class SnipServer {
    private val support: PropertyChangeSupport = PropertyChangeSupport(this)

    fun registerSession(session: SnipUserSession) {
        support.addPropertyChangeListener(session)
    }

    fun removeSession(session: SnipUserSession) {
        support.removePropertyChangeListener(session)
    }

    fun snipUpdated(snip: SnipDc) { //snip ID and UN as params here prob
        support.firePropertyChange("${snip.username}:${snip.id}:${snip.editingSessionId}",
            "Prior value not tracked! This property should never be called", snip) //need to track values here? ugh
    }
}