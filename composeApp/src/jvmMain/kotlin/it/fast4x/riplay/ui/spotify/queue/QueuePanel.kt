package it.fast4x.riplay.ui.spotify.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.riplay.player.QueueItem
import it.fast4x.riplay.player.QueuePlayer
import it.fast4x.riplay.ui.spotify.RiPlayColors

/**
 * Right-hand side panel listing the play queue, Spotify style. Self-contained: reads
 * [QueuePlayer.queue] directly, edits it through [QueueEditable] (see QueueOps.kt for why that's
 * a separate adapter rather than new QueuePlayer methods).
 */
@Composable
fun QueuePanel(player: QueuePlayer, onClose: () -> Unit) {
    val queue by player.queue.collectAsState()
    val editable = remember(player) { player.asEditable() }
    val items = queue.items
    val currentIndex = queue.index
    val current = queue.current
    val upcoming = if (currentIndex >= 0) items.withIndex().drop(currentIndex + 1) else emptyList()

    Column(
        Modifier
            .width(360.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(RiPlayColors.panel),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "File d'attente",
                color = RiPlayColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconTapButton(Icons.Filled.Close, "Fermer", onClose)
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (current != null) {
                item { SectionLabel("En cours de lecture") }
                item {
                    QueueRow(
                        item = current,
                        isCurrent = true,
                        onPlay = { editable.playAt(currentIndex) },
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item { SectionLabel("À suivre") }
                itemsIndexed(upcoming, key = { _, (actualIndex, _) -> actualIndex }) { i, (actualIndex, item) ->
                    QueueRow(
                        item = item,
                        isCurrent = false,
                        onPlay = { editable.playAt(actualIndex) },
                        onRemove = { editable.removeAt(actualIndex) },
                        onMoveUp = if (i > 0) ({ editable.move(actualIndex, actualIndex - 1) }) else null,
                        onMoveDown = if (i < upcoming.lastIndex) ({ editable.move(actualIndex, actualIndex + 1) }) else null,
                    )
                }
            }

            if (current == null && upcoming.isEmpty()) {
                item {
                    Text(
                        "File d'attente vide",
                        color = RiPlayColors.textTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label,
        color = RiPlayColors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hovered) RiPlayColors.cardHover else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onPlay() } }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(RiPlayColors.surface)) {
            item.thumbnailUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                item.title ?: item.videoId,
                color = if (isCurrent) RiPlayColors.accent else RiPlayColors.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.artist?.let {
                Text(it, color = RiPlayColors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (isCurrent) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Lecture en cours", tint = RiPlayColors.accent, modifier = Modifier.size(16.dp))
        } else {
            // ponytail: fixed-width trailing controls (alpha-toggled, not conditionally composed) so
            // the row doesn't reflow when the pointer enters/leaves it.
            Row(
                Modifier.width(78.dp).alpha(if (hovered) 1f else 0f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onMoveUp?.let { IconTapButton(Icons.Filled.KeyboardArrowUp, "Monter", it, size = 16.dp) }
                onMoveDown?.let { IconTapButton(Icons.Filled.KeyboardArrowDown, "Descendre", it, size = 16.dp) }
                onRemove?.let { IconTapButton(Icons.Filled.Close, "Retirer", it, size = 14.dp) }
            }
        }
    }
}

@Composable
private fun IconTapButton(icon: ImageVector, label: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 18.dp) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Icon(
        icon,
        contentDescription = label,
        tint = if (hovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
        modifier = Modifier
            .size(size)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
    )
}
