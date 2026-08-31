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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import database.DB
import database.entities.Playlist
import database.entities.PlaylistPreview
import it.fast4x.riplay.ui.spotify.RiPlayColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NEW_PLAYLIST_NAME = "Nouvelle playlist"

/**
 * Local playlists: list, create, rename, delete. Reads `playlistPreviewsByNameAsc()` so each row
 * carries its song count without a query per row.
 *
 * Rename and delete happen inline rather than in a dialog: a modal over a Compose Desktop window
 * costs a second window on some Linux WMs, and the row already has the space.
 */
@Composable
fun PlaylistsTab(onOpenPlaylist: (Long) -> Unit) {
    var playlists by remember { mutableStateOf<List<PlaylistPreview>>(emptyList()) }
    // Replace, never append: the Flow re-emits the whole list on every write, and a duplicate
    // key crashes the LazyColumn.
    LaunchedEffect(Unit) { DB.playlistPreviewsByNameAsc().collect { playlists = it } }

    // Id of the row being renamed, or of the row asking to confirm a delete. Never both.
    var editingId by remember { mutableStateOf<Long?>(null) }
    var confirmingId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        NewPlaylistButton {
            scope.launch {
                val id = withContext(Dispatchers.IO) { DB.insert(Playlist(name = NEW_PLAYLIST_NAME)) }
                // Straight into rename: a list of identically named playlists is useless.
                // Assigned back on the composition's dispatcher, not on the IO one.
                if (id > 0) editingId = id
            }
        }

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune playlist. Crées-en une pour commencer.",
                    color = RiPlayColors.textSecondary,
                    fontSize = 14.sp,
                )
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(playlists, key = { it.playlist.id }) { preview ->
                val id = preview.playlist.id
                when {
                    editingId == id -> RenameRow(
                        initial = preview.playlist.name,
                        onCancel = { editingId = null },
                        onConfirm = { name ->
                            editingId = null
                            scope.launch(Dispatchers.IO) { DB.renamePlaylist(id, name) }
                        },
                    )

                    confirmingId == id -> ConfirmDeleteRow(
                        name = preview.playlist.name,
                        onCancel = { confirmingId = null },
                        onConfirm = {
                            confirmingId = null
                            scope.launch(Dispatchers.IO) { DB.deletePlaylist(id) }
                        },
                    )

                    else -> PlaylistRow(
                        preview = preview,
                        onClick = { onOpenPlaylist(id) },
                        onRename = { editingId = id },
                        onDelete = { confirmingId = id },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewPlaylistButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(500.dp))
            .background(if (hovered) RiPlayColors.cardHover else RiPlayColors.surface)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Add, null, tint = RiPlayColors.textPrimary, modifier = Modifier.size(18.dp))
        Text("Nouvelle playlist", color = RiPlayColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlaylistRow(
    preview: PlaylistPreview,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
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
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(RiPlayColors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.List, null, tint = RiPlayColors.textTertiary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                preview.playlist.name,
                color = RiPlayColors.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (preview.songCount == 1) "1 titre" else "${preview.songCount} titres",
                color = RiPlayColors.textSecondary,
                fontSize = 12.sp,
            )
        }
        // Actions stay hidden until hover, so the list reads as titles rather than as a toolbar.
        if (hovered) {
            RowAction(Icons.Filled.Edit, "Renommer", onRename)
            RowAction(Icons.Filled.Delete, "Supprimer", onDelete)
        }
    }
}

@Composable
private fun RenameRow(initial: String, onCancel: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial) }
    // An empty name would leave an unclickable blank row behind.
    fun submit() = name.trim().takeIf { it.isNotEmpty() }?.let(onConfirm) ?: onCancel()

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = RiPlayColors.surface,
                unfocusedContainerColor = RiPlayColors.surface,
                focusedTextColor = RiPlayColors.textPrimary,
                unfocusedTextColor = RiPlayColors.textPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = RiPlayColors.accent,
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
        )
        RowAction(Icons.Filled.Check, "Valider") { submit() }
        RowAction(Icons.Filled.Close, "Annuler", onCancel)
    }
}

@Composable
private fun ConfirmDeleteRow(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(RiPlayColors.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Supprimer « $name » ?",
            color = RiPlayColors.textPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextAction("Supprimer", RiPlayColors.accent, onConfirm)
        Box(Modifier.width(4.dp))
        TextAction("Annuler", RiPlayColors.textSecondary, onCancel)
    }
}

@Composable
private fun RowAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Icon(
        icon,
        contentDescription = description,
        tint = if (hovered) RiPlayColors.textPrimary else RiPlayColors.textSecondary,
        modifier = Modifier
            .size(18.dp)
            .hoverable(interaction)
            // Consume the tap: the row underneath opens the playlist.
            .pointerInput(Unit) { detectTapGestures { onClick() } },
    )
}

@Composable
private fun TextAction(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClick() } },
    )
}
