package it.fast4x.riplay.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import database.MusicDatabaseDesktop
import database.entities.Song
import it.fast4x.environment.Environment
import it.fast4x.riplay.enums.PageType
import it.fast4x.riplay.player.DebugControlServer
import it.fast4x.riplay.player.QueueItem
import it.fast4x.riplay.player.QueuePlayer
import it.fast4x.riplay.player.webview.CefPlayerController
import it.fast4x.riplay.player.webview.CefRuntime
import it.fast4x.riplay.ui.screens.AlbumScreen
import it.fast4x.riplay.ui.screens.ArtistScreen
import it.fast4x.riplay.ui.screens.MoodScreen
import it.fast4x.riplay.ui.screens.PlaylistScreen
import it.fast4x.riplay.ui.screens.QuickPicsScreen
import it.fast4x.riplay.ui.spotify.NavSection
import it.fast4x.riplay.ui.spotify.SpotifyShell
import it.fast4x.riplay.ui.spotify.library.LibraryScreen
import it.fast4x.riplay.ui.spotify.search.SearchScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ThreeColumnsApp() {

    val db = remember { MusicDatabaseDesktop }
    val coroutineScope by remember { mutableStateOf(CoroutineScope(Dispatchers.IO)) }

    var videoId by remember { mutableStateOf("") }
    var nowPlayingSong by remember { mutableStateOf<Song?>(null) }
    var artistId by remember { mutableStateOf("") }
    var albumId by remember { mutableStateOf("") }
    var playlistId by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<Environment.Mood.Item?>(null) }

    // Embedded Chromium plays every track (it drives the real YouTube Music page), unlike stream
    // extraction which 403s on label content. Initialized off the UI thread; the controller exists
    // only once the ~150 MB runtime is ready.
    val cefStatus by CefRuntime.state.collectAsState()
    LaunchedEffect(Unit) { kotlinx.coroutines.withContext(Dispatchers.IO) { CefRuntime.ensureInitialized() } }
    val player = remember(cefStatus) {
        if (cefStatus == CefRuntime.Status.READY) QueuePlayer(CefPlayerController()) else null
    }

    // Debug/HTTP path only: DebugControlServer pushes a videoId, we load it (metadata-less).
    LaunchedEffect(videoId, player) {
        if (videoId.isEmpty() || player == null) return@LaunchedEffect
        nowPlayingSong = db.getSong(videoId)
        player.load(videoId)
    }

    // Development control surface (-Priplay.control): drive the player and read state over HTTP.
    LaunchedEffect(player) {
        DebugControlServer.player = player
        DebugControlServer.startIfEnabled()
    }
    LaunchedEffect(Unit) {
        DebugControlServer.requestedVideo.collect { requested -> requested?.let { videoId = it } }
    }

    var showPageType by remember { mutableStateOf(PageType.QUICKPICS) }
    var activeSection by remember { mutableStateOf(NavSection.HOME) }

    // Clicking a song plays it now with full metadata so the now-playing bar has cover/title/artist.
    fun playSong(song: Song) {
        player?.playNow(listOf(QueueItem(song.id, song.title, song.artistsText, song.thumbnailUrl)))
        coroutineScope.launch { db.upsert(song) }
    }

    // Album/playlist/artist: clicking a track queues the whole list from that position, like Spotify.
    fun playSongs(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        player?.playNow(songs.map { QueueItem(it.id, it.title, it.artistsText, it.thumbnailUrl) }, index)
        songs.getOrNull(index)?.let { s -> coroutineScope.launch { db.upsert(s) } }
    }

    SpotifyShell(
        player = player,
        activeSection = activeSection,
        onSelectSection = { section ->
            activeSection = section
            showPageType = PageType.QUICKPICS // leave any deep page
        },
    ) {
        when {
            showPageType == PageType.ALBUM -> ScrollContent {
                AlbumScreen(
                    browseId = albumId,
                    onSongClick = ::playSongs,
                    onAlbumClick = { albumId = it; showPageType = PageType.ALBUM },
                )
            }

            showPageType == PageType.ARTIST -> ScrollContent {
                ArtistScreen(
                    browseId = artistId,
                    onSongClick = ::playSongs,
                    onPlaylistClick = { playlistId = it; showPageType = PageType.PLAYLIST },
                    onViewAllAlbumsClick = {},
                    onViewAllSinglesClick = {},
                    onAlbumClick = { albumId = it; showPageType = PageType.ALBUM },
                    onClosePage = { showPageType = PageType.QUICKPICS },
                )
            }

            showPageType == PageType.PLAYLIST -> ScrollContent {
                PlaylistScreen(
                    browseId = playlistId,
                    onSongClick = ::playSongs,
                    onAlbumClick = { albumId = it; showPageType = PageType.ALBUM },
                    onClosePage = { showPageType = PageType.QUICKPICS },
                )
            }

            showPageType == PageType.MOOD -> ScrollContent {
                mood?.let {
                    MoodScreen(
                        mood = it,
                        onAlbumClick = { id -> albumId = id; showPageType = PageType.ALBUM },
                        onArtistClick = { id -> artistId = id; showPageType = PageType.ARTIST },
                        onPlaylistClick = { id -> playlistId = id; showPageType = PageType.PLAYLIST },
                    )
                }
            }

            activeSection == NavSection.LIBRARY -> LibraryScreen(
                onPlay = { player?.playNow(listOf(it)) },
                onOpenAlbum = { albumId = it.id; showPageType = PageType.ALBUM },
                onOpenArtist = { artistId = it; showPageType = PageType.ARTIST },
                onOpenPlaylist = {}, // ponytail: local playlists tab is empty-state; no browseId to open yet
            )

            activeSection == NavSection.SEARCH -> SearchScreen(
                onPlay = { player?.playNow(listOf(it)) },
            )

            else -> ScrollContent { // HOME / QUICKPICS
                QuickPicsScreen(
                    onSongClick = ::playSong,
                    onAlbumClick = { albumId = it; showPageType = PageType.ALBUM },
                    onArtistClick = { artistId = it; showPageType = PageType.ARTIST },
                    onPlaylistClick = { playlistId = it; showPageType = PageType.PLAYLIST },
                    onMoodClick = { mood = it; showPageType = PageType.MOOD },
                )
            }
        }
    }
}

/** Vertical-scroll container matching the old CenterPanelContent so the reused screens behave. */
@Composable
private fun ScrollContent(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        content()
    }
}

