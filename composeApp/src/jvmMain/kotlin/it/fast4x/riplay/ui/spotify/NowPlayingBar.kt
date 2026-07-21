package it.fast4x.riplay.ui.spotify

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import extension.formatTimestamp
import it.fast4x.riplay.player.QueuePlayer
import it.fast4x.riplay.player.RepeatMode
import kotlin.math.roundToInt

/** Bottom transport bar, full width, ~88dp. Only shown by the shell when a track is loaded. */
@Composable
fun NowPlayingBar(player: QueuePlayer) {
    val playback by player.playback.collectAsState()
    val queue by player.queue.collectAsState()
    val current = queue.current ?: return

    Row(
        Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RiPlayColors.panel)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: cover + title/artist + like
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(RiPlayColors.surface),
            ) {
                current.thumbnailUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Box(Modifier.weight(1f, fill = false)) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        current.title ?: current.videoId,
                        color = RiPlayColors.textPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    current.artist?.let {
                        Text(
                            it,
                            color = RiPlayColors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "J'aime",
                tint = RiPlayColors.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        // Center: controls + seekbar
        androidx.compose.foundation.layout.Column(
            Modifier.width(560.dp).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                ControlIcon(Icons.Filled.Shuffle, "Aléatoire", active = queue.shuffle) { player.toggleShuffle() }
                ControlIcon(Icons.Filled.SkipPrevious, "Précédent") { player.previous() }
                // Main play/pause: white pill, black glyph (the one shadow in the UI).
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RiPlayColors.textPrimary)
                        .pointerInput(playback.isPlaying) {
                            detectTapGestures { if (playback.isPlaying) player.pause() else player.play() }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Lecture",
                        tint = RiPlayColors.onAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                ControlIcon(Icons.Filled.SkipNext, "Suivant") { player.next() }
                val repeatActive = queue.repeat != RepeatMode.OFF
                ControlIcon(
                    if (queue.repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Répéter",
                    active = repeatActive,
                ) { player.cycleRepeat() }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    playback.timestamp.formatTimestamp(),
                    color = RiPlayColors.textTertiary,
                    fontSize = 11.sp,
                )
                val duration = playback.duration.coerceAtLeast(0)
                val fraction = if (duration > 0) playback.timestamp.toFloat() / duration else 0f
                RiSlider(
                    fraction = fraction,
                    onScrub = { if (duration > 0) player.seekTo((it * duration).roundToInt().toLong()) },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    playback.duration.formatTimestamp(),
                    color = RiPlayColors.textTertiary,
                    fontSize = 11.sp,
                )
            }
        }

        // Right: volume
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            val volIcon = when {
                playback.isMuted || playback.volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                playback.volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                else -> Icons.AutoMirrored.Filled.VolumeUp
            }
            ControlIcon(volIcon, "Son") { player.toggleSound() }
            RiSlider(
                fraction = playback.volume,
                onScrub = { player.setVolume(it) },
                modifier = Modifier.width(130.dp),
            )
        }
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint = when {
        active -> RiPlayColors.accent
        hovered -> RiPlayColors.textPrimary
        else -> RiPlayColors.textSecondary
    }
    Icon(
        icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier
            .size(20.dp)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
    )
}

/**
 * Lightweight draggable slider: rail + green fill + white knob (knob shown only on hover/drag).
 * No permanent animation. [fraction] and the scrub result are both 0f..1f.
 */
@Composable
private fun RiSlider(
    fraction: Float,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
) {
    var widthPx by remember { mutableStateOf(0f) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shown = (dragFraction ?: fraction).coerceIn(0f, 1f)

    Box(
        modifier
            .height(16.dp)
            .hoverable(interaction)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { off -> if (widthPx > 0) onScrub((off.x / widthPx).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off -> if (widthPx > 0) dragFraction = (off.x / widthPx).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, _ ->
                        if (widthPx > 0) dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = { dragFraction?.let(onScrub); dragFraction = null },
                    onDragCancel = { dragFraction = null },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(trackHeight).clip(CircleShape).background(RiPlayColors.rail))
        Box(Modifier.fillMaxWidth(shown).height(trackHeight).clip(CircleShape).background(RiPlayColors.accent))
        if (hovered || dragFraction != null) {
            Box(
                Modifier
                    .offset { IntOffset((shown * widthPx - 6.dp.toPx()).roundToInt(), 0) }
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(RiPlayColors.textPrimary),
            )
        }
    }
}
