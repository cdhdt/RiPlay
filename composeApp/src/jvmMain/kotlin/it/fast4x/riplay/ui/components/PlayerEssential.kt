package it.fast4x.riplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import database.entities.Song
import it.fast4x.riplay.items.SongItem
import it.fast4x.riplay.ui.bars.DefaultBottomBar
import it.fast4x.riplay.utils.LoadImage
import player.PlayerController

@Composable
fun MiniPlayer(
    controller: PlayerController,
    song: Song?,
    onExpandAction: () -> Unit
){

    Column (verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        HorizontalDivider(
            color = Color.DarkGray,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth().alpha(0.6f)
        )
        Row(
            Modifier.background(Color.Black).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongItem(
                    thumbnailContent = {
                        song?.thumbnailUrl?.let {
                            LoadImage(it)
                        }
                    },
                    authors = song?.artistsText,
                    duration = song?.durationText,
                    title = song?.title,
                    thumbnailSizeDp = 80.dp,
                    modifier = Modifier.fillMaxWidth(0.2f)
                )
            }
            // Audio-only: no video frame to render (playback lives in the hidden CEF browser),
            // just the transport bound to the controller state.
            DefaultBottomBar(Modifier.fillMaxWidth(0.8f), controller)
        }
    }

}
