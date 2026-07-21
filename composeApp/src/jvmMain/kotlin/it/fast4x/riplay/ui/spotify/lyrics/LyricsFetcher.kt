package it.fast4x.riplay.ui.spotify.lyrics

import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.lyrics
import it.fast4x.lrclib.LrcLib
import kotlinx.serialization.ExperimentalSerializationApi

/** One parsed LRC line: offset from track start, in ms, and the lyric text. */
data class LyricsLine(val timeMs: Long, val text: String)

sealed interface LyricsContent {
    data class Synced(val lines: List<LyricsLine>) : LyricsContent
    data class Plain(val text: String) : LyricsContent
    data object Unavailable : LyricsContent
}

/**
 * G1: fetches lyrics for a track — LrcLib synced first, Environment (innertube) plain text as
 * fallback. Mirrors the provider order used by the Android app's Lyrics screen.
 *
 * ponytail: cache is an in-memory Map keyed by videoId, not the Room `Lyrics` entity — wiring
 * persistence would require touching the DB/DAO layer outside ui/spotify/lyrics. Upgrade path:
 * back this with the `database.entities.Lyrics` table if lyrics need to survive app restarts.
 */
object LyricsFetcher {
    private val cache = mutableMapOf<String, LyricsContent>()

    suspend fun fetch(videoId: String, artist: String?, title: String): LyricsContent {
        cache[videoId]?.let { return it }
        val result = fetchSynced(artist, title) ?: fetchPlain(videoId) ?: LyricsContent.Unavailable
        cache[videoId] = result
        return result
    }

    private suspend fun fetchSynced(artist: String?, title: String): LyricsContent.Synced? {
        if (artist.isNullOrBlank()) return null
        val tracks = LrcLib.lyrics(artist, title)?.getOrNull() ?: return null
        val synced = tracks.firstOrNull { !it.syncedLyrics.isNullOrBlank() }?.syncedLyrics ?: return null
        val lines = LrcLib.Lyrics(synced).sentences
            .filter { it.second.isNotBlank() }
            .map { LyricsLine(it.first, it.second.trim()) }
        return lines.takeIf { it.isNotEmpty() }?.let { LyricsContent.Synced(it) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun fetchPlain(videoId: String): LyricsContent.Plain? {
        val text = Environment.lyrics(NextBody(videoId = videoId))?.getOrNull() ?: return null
        return text.takeIf { it.isNotBlank() }?.let { LyricsContent.Plain(it) }
    }
}
