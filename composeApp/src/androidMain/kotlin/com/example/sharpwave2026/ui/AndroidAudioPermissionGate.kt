package com.example.sharpwave2026.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AndroidAudioPermissionGate(
    onGranted: () -> Unit
) {
    val context = LocalContext.current
}