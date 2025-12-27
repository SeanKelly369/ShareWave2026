package com.example.sharpwave2026.player

import kotlinx.coroutines.flow.Flow

data class Track (
    val id: String,
    val title: String,
    val artist: String = "",
    val uri: String = ""
)

data class PlayerState (
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val current: Track? get() = queue.getOrNull(index)
}

interface Player {
    val state: Flow<PlayerState>

    fun setQueue(tracks: List<Track>, startIndex: Int = 0)
    fun play()
    fun pause()
    fun toggle()
    fun next()
    fun prev()

    fun seekTo(positionMs: Long)
}