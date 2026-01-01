package com.example.sharpwave2026.library

import com.example.sharpwave2026.player.Track

data class PlayList(
    val id: String,
    val name: String,
    val tracks: List<Track>,
    val kind: Kind = Kind.Auto
) {
    enum class Kind { Auto, Manual }
}
