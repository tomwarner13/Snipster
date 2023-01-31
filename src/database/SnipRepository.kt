package snipster.database

import snipster.cache.CacheProvider
import snipster.schema.Snip
import snipster.schema.SnipDc
import snipster.schema.Snips
import snipster.server.SnipServer
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

class SnipRepository(private val application: Application) : DatabaseBase(application) {
    private val _cache by di{application}.instance<CacheProvider>()

    suspend fun getSnipsByUser(username: String) : List<Snip> { //maybe change return type to the native iterator thing for callers to use?
        val result = mutableListOf<Snip>()
        dbExec {
            result.addAll(Snip.find { Snips.username eq username }.orderBy(Snips.createdOn to SortOrder.ASC) )
        }
        if(result.isEmpty()) {
            result.add(createSnip(username, "untitled",""))
        }
        _cache.putObject("ownedSnips:$username", result.map { it.id }.toMutableSet())
        //TODO this probably deserves some attention, i'm writing some stuff INTO the cache but mostly not, it appears, using it for anything
        return result
    }
/*
    fun getOwnedSnips(username: String) : MutableSet<Int> {
        return _cache.getOrFetchObject("ownedSnips:$username") {
            return@getOrFetchObject transaction {
                return@transaction Snip.find { Snips.username eq username }.map { it.id.value }.toMutableSet()
            }
        }
    }
 */

    suspend fun getOwnedSnips(username: String) : MutableSet<Int> {
        return _cache.getOrFetchObjectAsync("ownedSnips:$username") {
            return@getOrFetchObjectAsync transaction {
                return@transaction Snip.find { Snips.username eq username }.map { it.id.value }.toMutableSet()
            }
        }
    }

    suspend fun getSnip(id: Int) : Snip { //does this work correctly when snip no exist?
        return dbExec {
            Snip[id]
        }
    }

    suspend fun createSnip(username: String, title: String, content: String): Snip {
        return dbExec {
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

            return@dbExec result
        }
    }

    suspend fun editSnip(dc: SnipDc) {
        dbExec {
            val snip = Snip[dc.id]
            if(snip.username != dc.username) throw SecurityException("Attempted to modify unowned snip!") //DEF UNIT TEST THIS
            snip.title = dc.title
            snip.content = dc.content
            snip.lastModified = DateTime.now()
        }
        //also notify observers by username that snip has been modified?
        _server.snipUpdated(dc)
    }

    suspend fun deleteSnip(id: Int, username: String) : Int { //or by ID and confirm user?
        val recordsDeleted = dbExec {
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
