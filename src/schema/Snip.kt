package com.okta.demo.ktor.schema

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime
import java.util.*

object Snips : IntIdTable("snips") {
    val username = varchar("username", 150)
    val title = varchar("title", 150)
    val content = text("content")
    val createdOn = datetime("created_on")
    val lastModified = datetime("last_modified")
}

class Snip(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Snip>(Snips)
    var username by Snips.username
    var title by Snips.title
    var content by Snips.content
    var createdOn by Snips.createdOn
    var lastModified by Snips.lastModified

    fun toDc() = SnipDc(
        id.value,
        username,
        title,
        content,
        createdOn.toDate(),
        lastModified.toDate()
    )
}

data class SnipDc(
    val id: Int,
    val username: String,
    val title: String,
    val content: String,
    val createdOn: Date,
    val lastModified: Date,
    var editingSessionId: String = ""
)
