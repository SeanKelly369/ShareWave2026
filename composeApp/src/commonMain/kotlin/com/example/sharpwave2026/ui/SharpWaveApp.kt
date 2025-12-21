package com.example.sharpwave2026.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sharpwave2026.player.*

@Composable
fun SharpWaveApp() {
    val player = remember { providePlayer() }
    val state by player.state.collectAsState(PlayerState())

    LaunchedEffect(Unit) {
        player.setQueue(
            listOf(
                Track("1", "Xurious The Second Coming"),
                Track("2", "Xurious The Stranger"),
                Track("3", "Xurious We Have Dreamed The Same Dream")
            ),
            startIndex = 0
        )
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
                    trailingContent = { if (idx == state.index ) Text(if (state.isPlaying) "▶" else "Ⅱ") }
                )
                Divider()
            }
        }
    }
}