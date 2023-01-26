package com.okta.demo.ktor.schema

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object UserSettingsTable : IdTable<String>("user_settings") {
    val username = varchar("username", 150).uniqueIndex()
    override val id: Column<EntityID<String>> = username.entityId()

    val useLineNumbers = bool("use_line_numbers")
    val insertClosing = bool("insert_closing")
}

class UserSettings(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserSettings>(UserSettingsTable)

    var useLineNumbers by UserSettingsTable.useLineNumbers
    var insertClosing by UserSettingsTable.insertClosing

    fun toDc() = UserSettingsDc(
        id.value,
        useLineNumbers,
        insertClosing
    )
}

data class UserSettingsDc(
    val username: String,
    val useLineNumbers: Boolean,
    val insertClosing: Boolean
)