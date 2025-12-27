package com.example.sharpwave2026.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class FakePlayer: Player {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null;

    private val _state = MutableStateFlow(PlayerState())
    override val state = _state.asStateFlow()

    private fun currentDurationMs(): Long = 180_000L // 3 minutes
    private val tickMs = 250L

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        stopTicker()

        val idx = if (tracks.isEmpty()) - 1 else startIndex.coerceIn(tracks.indices)

        _state.value = PlayerState(
            queue = tracks,
            index = idx,
            isPlaying = false,
            positionMs = 0L,
            durationMs = if (idx >= 0) currentDurationMs() else 0L
        )
    }

    override fun play() {
        val s = _state.value
        if (s.queue.isEmpty() || s.index !in s.queue.indices) return

        _state.value = s.copy(isPlaying = true)
        startTicker()
    }

    override fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        stopTicker()
    }

    override fun toggle() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return

        val nextIndex = (s.index + 1).coerceAtMost(s.queue.lastIndex)
        val isSame = nextIndex == s.index

        _state.value = s.copy(
            index = nextIndex,
            positionMs = 0L,
            durationMs = if (!isSame) currentDurationMs() else s.durationMs
        )

        if (s.isPlaying) startTicker()
    }

    override fun prev() {
        val s = _state.value
        if (s.queue.isEmpty()) return

        val prevIndex = (s.index - 1).coerceAtLeast(0)
        val isSame = prevIndex == s.index

        _state.value = s.copy(
            index = prevIndex,
            positionMs = 0L,
            durationMs = if (!isSame) currentDurationMs() else s.durationMs
        )

        if (s.isPlaying) startTicker()
    }

    override fun seekTo(positionMs: Long) {
        val s = _state.value
        if (s.queue.isEmpty() || s.index !in s.queue.indices) return

        val dur = s.durationMs.takeIf { it > 0 } ?: currentDurationMs()
        val clamped = positionMs.coerceIn(0L..dur)

        _state.value = s.copy(positionMs = clamped, durationMs = dur)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(tickMs)

                val s = _state.value
                if (!s.isPlaying) continue
                if (s.queue.isEmpty() || s.index !in s.queue.indices) continue

                val dur = s.durationMs.takeIf { it > 0 } ?: currentDurationMs()
                val nextPos = s.positionMs + tickMs

                if (nextPos >= dur) {
                    // track finished -> auto-advance
                    if (s.index < s.queue.lastIndex) {
                        _state.value = s.copy(
                            positionMs = dur,
                            isPlaying = false,
                            durationMs = dur
                        )
                        stopTicker()
                    }
                } else {
                    _state.value = s.copy(positionMs = nextPos, durationMs = dur)
                }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null

    }

    fun dispose() {
        stopTicker()
        scope.cancel()

    }
}