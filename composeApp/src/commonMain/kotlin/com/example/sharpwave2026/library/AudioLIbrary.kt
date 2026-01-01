package com.example.sharpwave2026.library

import androidx.compose.runtime.Composable
import com.example.sharpwave2026.player.Track

interface AudioLibrary {
    suspend fun scanTracks(): List<Track>
}

@Composable
expect fun provideAudioLibrary(): AudioLibrary