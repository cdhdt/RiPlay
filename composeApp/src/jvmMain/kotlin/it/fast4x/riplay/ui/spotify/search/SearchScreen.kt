package it.fast4x.riplay.ui.spotify.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.SearchBody
import it.fast4x.environment.requests.searchPage
import it.fast4x.environment.utils.from
import it.fast4x.riplay.player.QueueItem
import it.fast4x.riplay.ui.spotify.RiPlayColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Online song search over InnerTube (same endpoint the Android app uses). Empty query shows recent
 * searches from [SearchHistory]; a click plays the track. Song results only for now.
 * ponytail: songs-only — add album/artist filter tabs (Environment.SearchFilter.*) when needed.
 */
@Composable
fun SearchScreen(onPlay: (QueueItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Environment.SongItem>>(emptyList()) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { recent = SearchHistory.recent() }

    // Debounced live search: wait for typing to settle before hitting the network.
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isEmpty()) { results = emptyList(); loading = false; return@LaunchedEffect }
        loading = true
        delay(350)
        results = withContext(Dispatchers.IO) {
            Environment.searchPage(
                body = SearchBody(query = q, params = Environment.SearchFilter.Song.value),
                fromMusicShelfRendererContent = Environment.SongItem.Companion::from,
            )?.getOrNull()?.items ?: emptyList()
        }
        loading = false
    }

    fun play(item: Environment.SongItem) {
        onPlay(QueueItem(item.key, item.title, item.authors?.joinToString(", ") { it.name.orEmpty() }, item.thumbnail?.url))
        scope.launch { SearchHistory.record(query.trim()); recent = SearchHistory.recent() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 12.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Que veux-tu écouter ?", color = RiPlayColors.textTertiary) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = RiPlayColors.textSecondary) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { query = query.trim() }),
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )

        when {
            query.isBlank() -> RecentSearches(recent, onPick = { query = it })
            loading && results.isEmpty() -> Text("Recherche…", color = RiPlayColors.textSecondary, fontSize = 14.sp)
            results.isEmpty() -> Text("Aucun résultat", color = RiPlayColors.textSecondary, fontSize = 14.sp)
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.key }) { ResultRow(it, onClick = { play(it) }) }
            }
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit) {
    if (recent.isEmpty()) {
        Text("Recherche récente vide", color = RiPlayColors.textSecondary, fontSize = 14.sp)
        return
    }
    Column {
        Text("Recherches récentes", color = RiPlayColors.textPrimary, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
        recent.forEach { q ->
            Text(
                q,
                color = RiPlayColors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().clickable { onPick(q) }.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ResultRow(item: Environment.SongItem, onClick: () -> Unit) {
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
            item.thumbnail?.url?.let {
                AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(end = 8.dp)) {
            Text(item.title.orEmpty(), color = RiPlayColors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            item.authors?.joinToString(", ") { it.name.orEmpty() }?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = RiPlayColors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
