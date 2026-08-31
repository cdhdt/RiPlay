package it.fast4x.riplay.ui.spotify.library

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import database.DB
import database.entities.Song
import it.fast4x.riplay.ui.spotify.RiPlayColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * One local playlist: its tracks in playlist order, play from any row, remove a row.
 *
 * Separate from `PlaylistScreen`, which browses a YouTube playlist by browseId. This one is backed
 * by SongPlaylistMap and keyed by the local autoincrement id.
 */
@Composable
fun LocalPlaylistScreen(
    playlistId: Long,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onClose: () -> Unit,
) {
    var songs by remember(playlistId) { mutableStateOf<List<Song>>(emptyList()) }
    var name by remember(playlistId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playlistId) { DB.playlistSongs(playlistId).collect { songs = it } }
    // The name comes off the same preview query the list uses, so there is no second DAO method
    // for it. first() and not collect(): a rename closes this screen anyway.
    LaunchedEffect(playlistId) {
        name = DB.playlistPreviewsByNameAsc().first()
            .firstOrNull { it.playlist.id == playlistId }?.playlist?.name.orEmpty()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = RiPlayColors.textSecondary,
                modifier = Modifier.size(22.dp).pointerInput(Unit) { detectTapGestures { onClose() } },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    name.ifEmpty { "Playlist" },
                    color = RiPlayColors.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (songs.size == 1) "1 titre" else "${songs.size} titres",
                    color = RiPlayColors.textSecondary,
                    fontSize = 13.sp,
                )
            }
            if (songs.isNotEmpty()) PlayAllButton { onPlaySongs(songs, 0) }
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 20.dp)) {
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Playlist vide. Ajoute des titres depuis ta bibliothèque.",
                        color = RiPlayColors.textSecondary,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        PlaylistTrackRow(
                            song = song,
                            onClick = { onPlaySongs(songs, index) },
                            onRemove = {
                                scope.launch(Dispatchers.IO) {
                                    DB.removeFromPlaylist(playlistId, song.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayAllButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(RiPlayColors.accent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Tout lire",
            tint = if (hovered) Color.White else Color.Black,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun PlaylistTrackRow(song: Song, onClick: () -> Unit, onRemove: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) RiPlayColors.cardHover else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(RiPlayColors.surface)) {
            song.thumbnailUrl?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.weight(1f)) {
            Text(song.title, color = RiPlayColors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            song.artistsText?.let {
                Text(it, color = RiPlayColors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (hovered) {
            val removeInteraction = remember { MutableInteractionSource() }
            val removeHovered by removeInteraction.collectIsHoveredAsState()
            Icon(
                Icons.Filled.Close,
                contentDescription = "Retirer de la playlist",
                tint = if (removeHovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .hoverable(removeInteraction)
                    .pointerInput(Unit) { detectTapGestures { onRemove() } },
            )
        }
        song.durationText?.let { Text(it, color = RiPlayColors.textTertiary, fontSize = 12.sp) }
    }
}
