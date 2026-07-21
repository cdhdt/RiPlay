package it.fast4x.riplay.ui.spotify.search

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import database.DB
import database.riplayDataDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Persists recent search queries in the Room `SearchQuery` table (see database/entities/SearchQuery.kt).
 *
 * ponytail: MusicDatabaseDao has no CRUD for SearchQuery, and this screen may only touch files under
 * ui/spotify/search/, so reads/writes go through a raw SQLite connection to the same db file instead of
 * a generated Dao. Upgrade path: add insert/query/delete for SearchQuery to MusicDatabaseDao and drop this.
 */
object SearchHistory {

    private val ensureMutex = Mutex()
    private var schemaEnsured = false

    private fun dbPath() = File(riplayDataDirectory(), "riplay.db").absolutePath

    /** Forces Room to open/migrate the database at least once, so the SearchQuery table is guaranteed to exist. */
    private suspend fun ensureSchema() {
        if (schemaEnsured) return
        ensureMutex.withLock {
            if (schemaEnsured) return
            runCatching { DB.getSong("") }
            schemaEnsured = true
        }
    }

    suspend fun recent(limit: Int = 10): List<String> = withContext(Dispatchers.IO) {
        ensureSchema()
        runCatching {
            val out = mutableListOf<String>()
            BundledSQLiteDriver().open(dbPath()).use { conn ->
                conn.prepare("SELECT query FROM SearchQuery ORDER BY id DESC LIMIT ?").use { stmt ->
                    stmt.bindLong(1, limit.toLong())
                    while (stmt.step()) out += stmt.getText(0)
                }
            }
            out
        }.getOrDefault(emptyList())
    }

    suspend fun record(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext
        ensureSchema()
        runCatching {
            BundledSQLiteDriver().open(dbPath()).use { conn ->
                // OR REPLACE bumps the row to a new (higher) id, keeping recency order on re-search.
                conn.prepare("INSERT OR REPLACE INTO SearchQuery(query) VALUES (?)").use { stmt ->
                    stmt.bindText(1, trimmed)
                    stmt.step()
                }
            }
        }
    }
}
