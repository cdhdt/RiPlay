package database.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

/**
 * A playlist plus the number of songs in it. Query projection, not a table — the count comes from
 * a GROUP BY over SongPlaylistMap, so listing playlists costs one query instead of one per row.
 */
@Immutable
data class PlaylistPreview(
    @Embedded val playlist: Playlist,
    val songCount: Int,
)
