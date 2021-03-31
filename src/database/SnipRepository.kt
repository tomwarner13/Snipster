package com.okta.demo.ktor.database

import com.okta.demo.ktor.cache.CacheProvider
import com.okta.demo.ktor.schema.Snip
import com.okta.demo.ktor.schema.SnipDc
import com.okta.demo.ktor.schema.Snips
import com.okta.demo.ktor.server.SnipServer
import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.slf4j.Logger
import java.util.*

class SnipRepository(private val application: Application) {
    private val _conn by di{application}.instance<DatabaseConnection>()
    private val _server by di{application}.instance<SnipServer>()
    private val _db : Database = _conn.db //not using these? or requires initialization and this is kind of a hack?
    private val _cache by di{application}.instance<CacheProvider>()
    private val _logger by di{application}.instance<Logger>()

    fun getSnipsByUser(username: String) : List<Snip> { //maybe change return type to the native iterator thing for callers to use?
        val result = mutableListOf<Snip>()
        transaction {
            result.addAll(Snip.find { Snips.username eq username }.orderBy(Snips.createdOn to SortOrder.ASC) )
        }
        if(result.isEmpty()) {
            result.add(createSnip(username, "untitled",""))
        }
        _cache.putObject("ownedSnips:$username", result.map { it.id }.toMutableSet())
        return result
    }

    fun getOwnedSnips(username: String) : MutableSet<Int> {
        return _cache.getOrFetchObject("ownedSnips:$username") {
            return@getOrFetchObject transaction {
                return@transaction Snip.find { Snips.username eq username }.map { it.id.value }.toMutableSet()
            }
        }
    }

    fun getSnip(id: Int) : Snip { //does this work correctly when snip no exist?
        return transaction {
            Snip[id]
        }
    }

    fun createSnip(username: String, title: String, content: String): Snip {
        return transaction {
            val result = Snip.new {
                this.username = username
                this.title = title
                this.content = content
                createdOn = DateTime.now()
                lastModified = DateTime.now()
            }

            _cache.getIfExists<MutableSet<Int>>("ownedSnips:$username")?.let {
                it.add(result.id.value)
                _cache.putObject("ownedSnips:$username", it)
            }

            _server.snipCreated(result.toDc())

            return@transaction result
        }
    }

    fun editSnip(dc: SnipDc) {
        transaction {
            val snip = Snip[dc.id]
            if(snip.username != dc.username) throw SecurityException("Attempted to modify unowned snip!") //DEF UNIT TEST THIS
            snip.title = dc.title
            snip.content = dc.content
            snip.lastModified = DateTime.now()
        }
        //also notify observers by username that snip has been modified?
        _server.snipUpdated(dc)
    }

    fun deleteSnip(id: Int, username: String) : Int { //or by ID and confirm user?
        val recordsDeleted = transaction {
            Snips.deleteWhere { Snips.id eq id and (Snips.username eq username) } //suspect this returns the total # of records deleted but need to confirm
        }

        val dc = SnipDc(
            id,
            username,
            "",
            "",
            Date(),
            Date(),
            "" //leave this blank so the event propagates to all sessions
        )

        _server.snipDeleted(dc)

        return recordsDeleted
    }

}
