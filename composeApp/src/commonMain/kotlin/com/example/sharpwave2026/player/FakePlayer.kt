package com.example.sharpwave2026.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlayer: Player {
    private val _state = MutableStateFlow(PlayerState())
    override val state = _state.asStateFlow()

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        val idx = if (tracks.isEmpty()) - 1 else startIndex.coerceIn(tracks.indices)
        _state.value = PlayerState(queue = tracks, index = idx, isPlaying = false)
    }

    override fun play() { _state.value = _state.value.copy(isPlaying = true) }
    override fun pause() { _state.value = _state.value.copy(isPlaying = false) }
    override fun toggle() { if (_state.value.isPlaying) pause() else play() }

    override fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        _state.value = s.copy(index = (s.index + 1).coerceAtMost(s.queue.lastIndex))
    }

    override fun prev() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        _state.value = s.copy(index = (s.index - 1).coerceAtLeast(0))
    }
}