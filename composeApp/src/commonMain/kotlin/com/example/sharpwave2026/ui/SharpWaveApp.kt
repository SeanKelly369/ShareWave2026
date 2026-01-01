package com.example.sharpwave2026.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sharpwave2026.library.provideAudioLibrary
import com.example.sharpwave2026.player.*
import com.example.sharpwave2026.utils.formatMs

@Composable
fun SharpWaveApp(
) {

    val player = providePlayer()
    val state by player.state.collectAsState(PlayerState())

    val library = provideAudioLibrary()

    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(reloadToken) {
        isLoading = true
        loadError = null
        try {
            val tracks = library.scanTracks()
            if (tracks.isNotEmpty()) {
                player.setQueue(tracks, startIndex = 0)
            } else {
                loadError = "No audio files found (or permission not granted yet)."
            }
        } catch (t: Throwable) {
            loadError = t.message ?: t.toString()
        } finally {
            isLoading = false
        }
    }

    val dur = state.durationMs.coerceAtLeast(1L)
    val pos = state.positionMs.coerceIn(0L, dur)

    var isDragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableStateOf(0L) }

    LaunchedEffect(pos, dur, isDragging) {
        if (!isDragging) dragMs = pos
    }

    MaterialTheme {
        Column(
            Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(8.dp)
        ) {
            Text(
                text = "Audio Arc",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height((8.dp)))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isLoading) {
                    Text("Scanning…", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Tracks: ${state.queue.size}", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedButton(onClick = { reloadToken++ }) {
                    Text("Reload")
                }
            }

            if (loadError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = loadError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF444444)
                )
            }

            Spacer(Modifier.height(8.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = state.current?.title ?: "Nothing selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = state.current?.artist ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = player::prev,
                            enabled = state.queue.isNotEmpty(),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) { Text("Prev") }

                        Button(
                            onClick = player::toggle,
                            enabled = state.queue.isNotEmpty(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(if (state.isPlaying) "Pause" else "Play")
                        }

                        OutlinedButton(
                            onClick = player::next,
                            enabled = state.queue.isNotEmpty(),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) { Text("Next") }
                    }

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
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(dragMs), style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                        Text(formatMs(dur),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF444444)
                        )
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
                HorizontalDivider()
            }
        }
    }
}