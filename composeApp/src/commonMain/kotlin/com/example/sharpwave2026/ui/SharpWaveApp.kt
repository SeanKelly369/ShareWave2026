package com.example.sharpwave2026.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sharpwave2026.player.*

@Composable
fun SharpWaveApp(
) {

    val player = remember { providePlayer() }
    val state by player.state.collectAsState(PlayerState())

    LaunchedEffect(Unit) {
        player.setQueue(
            listOf(
                Track("xurious_the_second_coming", "Xurious The Second Coming", artist = "Xurious"),
                Track("xurious_the_stranger", "Xurious The Stranger", artist = "Xurious"),
                Track("xurious_we_have_dreamed_the_same_dream", "Xurious We Have Dreamed The Same Dream", artist = "Xurious")
            ),
            startIndex = 0
        )
    }

    val dur = state.durationMs.coerceAtLeast(1L)
    val pos = state.positionMs.coerceIn(0L, dur)

    var isDragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableStateOf(0L) }

    LaunchedEffect(pos, dur, isDragging) {
        if (!isDragging) dragMs = pos
    
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("SharpWave", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height((12.dp)))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(state.current?.title ?: "Nothing selected", style = MaterialTheme.typography.titleMedium)
                    Text(state.current?.artist ?: "", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = player::prev, enabled = state.queue.isNotEmpty()) { Text("Prev")}
                        Button(onClick = player::toggle, enabled = state.queue.isNotEmpty()) {
                            Text(if (state.isPlaying) "Pause" else "Play")
                        }
                        OutlinedButton(onClick = player::next, enabled = state.queue.isNotEmpty()) { Text("Next")}
                    }
                    Spacer(Modifier.height(12.dp))

                    val dur = state.durationMs.coerceAtLeast(1L)
                    val pos = state.positionMs.coerceIn(0L, dur)

                    var isDragging by remember { mutableStateOf(false) }
                    var dragMs by remember { mutableStateOf(0L) }

                    LaunchedEffect(pos, dur, isDragging) {
                        if (!isDragging) dragMs = pos
                    }

                    Slider(
                        value = dragMs.toFloat() / dur.toFloat(),
                        onValueChange = { frac ->
                            isDragging = true
                            dragMs = (frac * dur).toLong()
                        },
                        onValueChangeFinished = {
                            isDragging = false
                            player.seekTo(dragMs)
                        },
                        enabled = state.queue.isNotEmpty() && dur > 1L,
                        modifier = Modifier.fillMaxWidth()

                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Queue", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            state.queue.forEachIndexed { idx,  t ->
                ListItem(
                    headlineContent = { Text(t.title) },
                    supportingContent = { Text(t.artist) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { player.setQueue(state.queue, idx ); player.play() },
                    trailingContent = { if (idx == state.index ) Text(if (state.isPlaying) "Ⅱ" else "▶") }
                )
                HorizontalDivider()
            }
        }
    }
}