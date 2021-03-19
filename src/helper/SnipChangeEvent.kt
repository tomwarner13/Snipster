package com.okta.demo.ktor.helper

import com.okta.demo.ktor.schema.ChangeType

class SnipChangeEvent (
    val id: Int,
    val type: ChangeType,
    val username: String,
    val sessionId: String) {


    companion object {
        fun fromPropertyName(propertyName: String) : SnipChangeEvent {
            val values = propertyName.split(':')
            if(values.size != 4) throw IllegalArgumentException("property name '$propertyName' was not in a correct format!")
            val id = values[0].toInt()
            val type = ChangeType.valueOf(values[1])
            val username = values[2]
            val sessionId = values[3]
            return SnipChangeEvent(id, type, username, sessionId)
        }
    }

    fun toPropertyName() = "$id:$type:$username:$sessionId"
}