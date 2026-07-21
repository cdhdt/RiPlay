package it.fast4x.riplay.ui.spotify

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.fast4x.riplay.player.QueuePlayer
import it.fast4x.riplay.ui.spotify.lyrics.LyricsPanel
import it.fast4x.riplay.ui.spotify.queue.QueuePanel
import org.jetbrains.compose.resources.painterResource
import riplay.composeapp.generated.resources.Res
import riplay.composeapp.generated.resources.app_icon

enum class NavSection { HOME, SEARCH, LIBRARY }

private enum class RightPanel { NONE, QUEUE, LYRICS }

/**
 * Spotify-style shell: sidebar + (topbar over content) as one row, now-playing bar full width below.
 * The engine wiring (player, queue, deep navigation) stays in the caller; this is pure UI.
 */
@Composable
fun SpotifyShell(
    player: QueuePlayer?,
    activeSection: NavSection,
    onSelectSection: (NavSection) -> Unit,
    content: @Composable () -> Unit,
) {
    val current = player?.queue?.collectAsState()?.value?.current
    var rightPanel by remember { mutableStateOf(RightPanel.NONE) }
    // A right panel only makes sense while something is loaded; drop it when playback clears.
    if (current == null && rightPanel != RightPanel.NONE) rightPanel = RightPanel.NONE

    Column(
        Modifier.fillMaxSize().background(RiPlayColors.bg).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sidebar(
                activeSection = activeSection,
                onSelectSection = onSelectSection,
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF1C231E),
                            0.35f to RiPlayColors.panel,
                        ),
                    ),
            ) {
                TopBar()
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            }
            if (player != null && current != null) {
                when (rightPanel) {
                    RightPanel.QUEUE -> QueuePanel(player) { rightPanel = RightPanel.NONE }
                    RightPanel.LYRICS -> LyricsPanel(player) { rightPanel = RightPanel.NONE }
                    RightPanel.NONE -> {}
                }
            }
        }
        if (player != null && current != null) {
            NowPlayingBar(
                player = player,
                queueOpen = rightPanel == RightPanel.QUEUE,
                lyricsOpen = rightPanel == RightPanel.LYRICS,
                onToggleQueue = { rightPanel = if (rightPanel == RightPanel.QUEUE) RightPanel.NONE else RightPanel.QUEUE },
                onToggleLyrics = { rightPanel = if (rightPanel == RightPanel.LYRICS) RightPanel.NONE else RightPanel.LYRICS },
            )
        }
    }
}

@Composable
private fun Sidebar(
    activeSection: NavSection,
    onSelectSection: (NavSection) -> Unit,
) {
    Column(
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(RiPlayColors.panel),
    ) {
        // Brand
        Row(
            Modifier
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 14.dp)
                .pointerInput(Unit) { detectTapGestures { onSelectSection(NavSection.HOME) } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(RiPlayColors.accent),
                modifier = Modifier.size(30.dp),
            )
            Row {
                Text("Ri", color = RiPlayColors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Play", color = RiPlayColors.accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Primary nav
        Column(
            Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            NavItem(Icons.Filled.Home, "Accueil", activeSection == NavSection.HOME) { onSelectSection(NavSection.HOME) }
            NavItem(Icons.Filled.Search, "Rechercher", activeSection == NavSection.SEARCH) { onSelectSection(NavSection.SEARCH) }
            NavItem(Icons.Filled.LibraryMusic, "Bibliothèque", activeSection == NavSection.LIBRARY) { onSelectSection(NavSection.LIBRARY) }
        }

        Spacer(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(RiPlayColors.border),
        )

        Text(
            "Ta bibliothèque",
            color = RiPlayColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 22.dp, bottom = 8.dp),
        )

        // ponytail: placeholder library list; wire to DB playlists/albums later.
        LazyColumn(
            Modifier.fillMaxHeight().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(placeholderLibrary) { item -> LibraryRow(item) }
        }
    }
}

private data class LibItem(val title: String, val subtitle: String, val gradient: Brush, val round: Boolean)

private fun grad(a: Long, b: Long) = Brush.linearGradient(listOf(Color(a), Color(b)))

private val placeholderLibrary = listOf(
    LibItem("Titres likés", "Playlist · 214 titres", grad(0xFF1E5F43, 0xFF0D2B20), false),
    LibItem("Chill Mix", "Playlist · RiPlay", grad(0xFF3A4A7A, 0xFF171B30), false),
    LibItem("Découvertes", "Playlist · RiPlay", grad(0xFF6A2F4D, 0xFF2A1220), false),
    LibItem("Daft Punk", "Artiste", grad(0xFF7A5A1E, 0xFF2E2410), true),
    LibItem("Random Access Memories", "Album · Daft Punk", grad(0xFF2F6A6A, 0xFF122626), false),
    LibItem("Focus profond", "Playlist · 88 titres", grad(0xFF503A7A, 0xFF1D1630), false),
    LibItem("Tame Impala", "Artiste", grad(0xFF7A3A2A, 0xFF2E1610), true),
    LibItem("Lo-fi du soir", "Playlist · 120 titres", grad(0xFF2A5A7A, 0xFF101F2E), false),
)

@Composable
private fun LibraryRow(item: LibItem) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) RiPlayColors.cardHover else Color.Transparent)
            .hoverable(interaction)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(if (item.round) CircleShape else RoundedCornerShape(6.dp))
                .background(item.gradient),
        )
        Column {
            Text(
                item.title,
                color = RiPlayColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.subtitle,
                color = RiPlayColors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) RiPlayColors.accentVeil else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val tint = if (active || hovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // History buttons (inert for now).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundButton(Icons.Filled.ChevronLeft, "Précédent")
            RoundButton(Icons.Filled.ChevronRight, "Suivant")
        }

        // Search field (local text only — no search screen wired yet).
        var query by remember { mutableStateOf("") }
        Row(
            Modifier
                .weight(1f, fill = false)
                .width(420.dp)
                .height(42.dp)
                .clip(CircleShape)
                .background(RiPlayColors.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = RiPlayColors.textSecondary, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("Que veux-tu écouter ?", color = RiPlayColors.textTertiary, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = RiPlayColors.textPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(RiPlayColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier.size(32.dp).clip(CircleShape).background(RiPlayColors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = "Profil", tint = RiPlayColors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(RiPlayColors.bg)
            .hoverable(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (hovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
