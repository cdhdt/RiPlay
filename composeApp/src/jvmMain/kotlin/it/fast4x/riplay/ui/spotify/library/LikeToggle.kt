package it.fast4x.riplay.ui.spotify.library

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import database.MusicDatabaseDao
import database.entities.Song
import it.fast4x.riplay.ui.spotify.RiPlayColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Song.likedAt: null = not liked, -1 = disliked, >0 = liked timestamp (see
 * commonutils/LikeUtils.kt + Song.toggleLike()). Persists via the existing Room DAO
 * `upsert(Song)` — no schema/DAO change needed.
 */
suspend fun likeSong(dao: MusicDatabaseDao, song: Song) {
    dao.upsert(song.toggleLike())
}

/** Fire-and-forget variant for click handlers (Compose `onClick` lambdas can't suspend). */
fun toggleLikeAsync(scope: CoroutineScope, dao: MusicDatabaseDao, song: Song) {
    scope.launch(Dispatchers.IO) { likeSong(dao, song) }
}

/**
 * Reusable heart toggle: filled green when liked, outlined grey otherwise (hover -> white).
 * Meant to be dropped on TrackRow-style lists and, later, the now-playing bar.
 */
@Composable
fun LikeButton(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val tint = when {
        liked -> RiPlayColors.accent
        hovered -> RiPlayColors.textPrimary
        else -> RiPlayColors.textSecondary
    }
    Icon(
        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        contentDescription = if (liked) "Ne plus aimer" else "Aimer",
        tint = tint,
        modifier = modifier
            .size(size)
            .hoverable(interaction)
            .pointerInput(liked) { detectTapGestures { onToggle() } },
    )
}
