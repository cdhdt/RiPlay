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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import database.entities.Album
import database.entities.Artist
import database.entities.SongEntity
import it.fast4x.riplay.player.QueueItem
import it.fast4x.riplay.ui.spotify.RiPlayColors

private enum class LibraryTab(val label: String) {
    SONGS("Titres"), PLAYLISTS("Playlists"), ALBUMS("Albums"), ARTISTS("Artistes")
}

/**
 * Spotify-style library screen: tabbed Titres / Playlists / Albums / Artistes, fed by the shared
 * Room DAO (Flows). Pure UI + navigation callbacks — playback and screen routing stay with the caller.
 */
@Composable
fun LibraryScreen(
    onPlay: (QueueItem) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
) {
    var tab by remember { mutableStateOf(LibraryTab.SONGS) }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("Bibliothèque", color = RiPlayColors.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.padding(top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryTab.entries.forEach { t -> Chip(t.label, active = t == tab) { tab = t } }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                LibraryTab.SONGS -> SongsTab(onPlay)
                LibraryTab.ALBUMS -> AlbumsTab(onOpenAlbum)
                LibraryTab.PLAYLISTS -> PlaylistsTab(onOpenPlaylist)
                LibraryTab.ARTISTS -> ArtistsTab(onOpenArtist)
            }
        }
    }
}

@Composable
private fun SongsTab(onPlay: (QueueItem) -> Unit) {
    var songs by remember { mutableStateOf<List<SongEntity>>(emptyList()) }
    // Flow re-emits the full list on every DB change; replace (not append) or the LazyColumn
    // crashes on duplicate keys the next time a like/upsert triggers a re-emit.
    LaunchedEffect(Unit) { DB.songsByTitleAsc().collect { songs = it } }
    var likedOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val visible = if (likedOnly) songs.filter { it.song.isLiked } else songs

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Tous", active = !likedOnly) { likedOnly = false }
            Chip("Titres aimés", active = likedOnly) { likedOnly = true }
        }
        if (visible.isEmpty()) {
            EmptyTab(if (likedOnly) "Aucun titre aimé pour le moment." else "Aucun titre pour le moment.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(visible, key = { it.song.id }) { entity ->
                    TrackRow(
                        entity = entity,
                        onClick = {
                            onPlay(
                                QueueItem(
                                    entity.song.id,
                                    entity.song.title,
                                    entity.song.artistsText,
                                    entity.song.thumbnailUrl,
                                ),
                            )
                        },
                        onToggleLike = { toggleLikeAsync(scope, DB, entity.song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(entity: SongEntity, onClick: () -> Unit, onToggleLike: () -> Unit) {
    val song = entity.song
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
        entity.albumTitle?.let {
            Text(
                it,
                color = RiPlayColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        LikeButton(liked = song.isLiked, onToggle = onToggleLike)
        song.durationText?.let { Text(it, color = RiPlayColors.textTertiary, fontSize = 12.sp) }
    }
}

@Composable
private fun AlbumsTab(onOpenAlbum: (Album) -> Unit) {
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    LaunchedEffect(Unit) { DB.getAllAlbums().collect { albums = it } }

    if (albums.isEmpty()) {
        EmptyTab("Aucun album pour le moment.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }) { album -> AlbumCard(album) { onOpenAlbum(album) } }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) RiPlayColors.cardHover else RiPlayColors.card)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(RiPlayColors.surface),
        ) {
            album.thumbnailUrl?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(
            album.title ?: "Album",
            color = RiPlayColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        album.authorsText?.let {
            Text(it, color = RiPlayColors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ArtistsTab(onOpenArtist: (String) -> Unit) {
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    // Followed artists, not every artist the catalogue ever mentioned: the DAO filters on
    // bookmarkedAt, which is what following sets.
    LaunchedEffect(Unit) { DB.bookmarkedArtists().collect { artists = it } }

    if (artists.isEmpty()) {
        EmptyTab("Aucun artiste suivi pour le moment.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(artists, key = { it.id }) { artist -> ArtistCard(artist) { onOpenArtist(artist.id) } }
    }
}

@Composable
private fun ArtistCard(artist: Artist, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) RiPlayColors.cardHover else RiPlayColors.card)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Round, unlike the square album art: the shape is how Spotify separates the two.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(RiPlayColors.surface),
        ) {
            artist.thumbnailUrl?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Text(
            artist.name ?: "Artiste",
            color = RiPlayColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyTab(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = RiPlayColors.textSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .clip(RoundedCornerShape(500.dp))
            .background(if (active) RiPlayColors.surface else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (active || hovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
