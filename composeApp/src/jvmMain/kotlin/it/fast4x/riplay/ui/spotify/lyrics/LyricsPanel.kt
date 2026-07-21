package it.fast4x.riplay.ui.spotify.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.player.QueuePlayer
import it.fast4x.riplay.ui.spotify.RiPlayColors

/**
 * G2: full lyrics panel for the track currently playing on [player]. Synced lyrics scroll and
 * highlight the active line from `player.playback.value.timestamp`; plain-text lyrics are a
 * simple scrollable column. [onClose] is the panel's own close affordance — the caller owns
 * whether/where this is shown (e.g. as an overlay or a side panel).
 */
@Composable
fun LyricsPanel(player: QueuePlayer, onClose: () -> Unit) {
    val queue by player.queue.collectAsState()
    val playback by player.playback.collectAsState()
    val current = queue.current

    var content by remember(current?.videoId) { mutableStateOf<LyricsContent?>(null) }

    LaunchedEffect(current?.videoId) {
        content = null
        val item = current ?: return@LaunchedEffect
        content = LyricsFetcher.fetch(item.videoId, item.artist, item.title ?: item.videoId)
    }

    Column(
        Modifier.fillMaxSize().background(RiPlayColors.panel).padding(24.dp),
    ) {
        PanelHeader(title = current?.title, artist = current?.artist, onClose = onClose)
        Spacer(Modifier.height(20.dp))
        Box(Modifier.weight(1f).fillMaxSize()) {
            when (val c = content) {
                null -> CenteredMessage("Chargement des paroles…")
                LyricsContent.Unavailable -> CenteredMessage("Paroles indisponibles")
                is LyricsContent.Plain -> PlainLyrics(c.text)
                is LyricsContent.Synced -> SyncedLyrics(c.lines, playback.timestamp)
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String?, artist: String?, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Paroles", color = RiPlayColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (title != null) {
                val subtitle = if (artist != null) "$title — $artist" else title
                Text(
                    subtitle,
                    color = RiPlayColors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(RiPlayColors.surface)
                .hoverable(interaction)
                .pointerInput(Unit) { detectTapGestures { onClose() } },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Fermer les paroles",
                tint = if (hovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricsLine>, timestampMs: Long) {
    val listState = rememberLazyListState()
    val activeIndex = lines.indexOfLast { it.timeMs <= timestampMs }.coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        listState.scrollToItem((activeIndex - 3).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(lines) { index, line ->
            val active = index == activeIndex
            Text(
                line.text,
                color = if (active) RiPlayColors.accent else RiPlayColors.textSecondary,
                fontSize = if (active) 22.sp else 18.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun PlainLyrics(text: String) {
    val lines = remember(text) { text.lines() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(lines) { line ->
            Text(
                line,
                color = RiPlayColors.textPrimary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = RiPlayColors.textSecondary, fontSize = 15.sp)
    }
}
