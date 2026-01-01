package com.example.sharpwave2026.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun provideAudioLibrary(): AudioLibrary {
    val ctx = LocalContext.current.applicationContext
    return remember { AndroidAudioLibrary(ctx) }
}