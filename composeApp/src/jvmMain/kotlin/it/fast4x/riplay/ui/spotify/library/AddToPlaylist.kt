package it.fast4x.riplay.ui.spotify.library

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.DB
import database.entities.Playlist
import database.entities.PlaylistPreview
import database.entities.Song
import database.entities.SongPlaylistMap
import it.fast4x.riplay.ui.spotify.RiPlayColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Appends a song to a playlist.
 *
 * The Song row is upserted first: SongPlaylistMap has a foreign key onto Song, so mapping a track
 * the library has never stored would be rejected. The insert itself is IGNORE-on-conflict and the
 * primary key is (songId, playlistId), so adding the same track twice is a no-op rather than a
 * duplicate row.
 */
suspend fun addSongToPlaylist(song: Song, playlistId: Long) {
    DB.upsert(song)
    val position = DB.nextPositionIn(playlistId)
    DB.insert(SongPlaylistMap(songId = song.id, playlistId = playlistId, position = position).default())
}

/**
 * The "+" on a track row: opens the list of local playlists, plus an entry that creates one and
 * drops the track straight into it.
 *
 * `open` is hoisted on purpose. Rows reveal their actions on hover, and moving the pointer onto the
 * menu takes it off the row — if the row owned this state it would drop the button, and the menu
 * with it, the moment you tried to click an entry.
 */
@Composable
fun AddToPlaylistButton(
    song: Song,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    var playlists by remember { mutableStateOf<List<PlaylistPreview>>(emptyList()) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scope = rememberCoroutineScope()

    // Read the list only while the menu is open; a Flow per track row would mean one active query
    // per visible row.
    LaunchedEffect(open) {
        if (open) playlists = DB.playlistPreviewsByNameAsc().first()
    }

    Box {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Ajouter à une playlist",
            tint = if (hovered || open) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
            modifier = modifier
                .size(size)
                .hoverable(interaction)
                .pointerInput(Unit) { detectTapGestures { onOpenChange(true) } },
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { onOpenChange(false) },
            modifier = Modifier.background(RiPlayColors.surface),
        ) {
            DropdownMenuItem(
                text = { Text("Nouvelle playlist", color = RiPlayColors.textPrimary, fontSize = 14.sp) },
                onClick = {
                    onOpenChange(false)
                    // Named after the track, the way Spotify names a playlist created from one.
                    scope.launch(Dispatchers.IO) {
                        val id = DB.insert(Playlist(name = song.title))
                        if (id > 0) addSongToPlaylist(song, id)
                    }
                },
            )
            playlists.forEach { preview ->
                DropdownMenuItem(
                    text = { Text(preview.playlist.name, color = RiPlayColors.textPrimary, fontSize = 14.sp) },
                    onClick = {
                        onOpenChange(false)
                        scope.launch(Dispatchers.IO) { addSongToPlaylist(song, preview.playlist.id) }
                    },
                )
            }
        }
    }
}
