package com.example.sharpwave2026.player

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class AndroidMediaPlayer(
    private val context: Context
) : Player {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private val tickMs = 200L

    private var mp: MediaPlayer? = null
    private var queue: List<Track> = emptyList()
    private var index: Int = -1

    private val _state = MutableStateFlow(PlayerState())
    override val state: Flow<PlayerState> = _state.asStateFlow()

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        queue = tracks
        index = if (tracks.isEmpty()) -1 else startIndex.coerceIn(tracks.indices)

        _state.value = PlayerState(
            queue = queue,
            index = index,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L
        )

        prepareCurrent(autoplay = false)
    }

    override fun play() {
        if (queue.isEmpty() || index !in queue.indices) return
        if (mp == null) {
            prepareCurrent(autoplay = true)
            return
        }
        mp?.start()
        _state.value = _state.value.copy(isPlaying = true)
        startTicker()
    }

    override fun pause() {
        mp?.pause()
        _state.value = _state.value.copy(isPlaying = false)
        stopTicker()
    }

    override fun toggle() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun next() {
        if (queue.isEmpty()) return
        if (index < queue.lastIndex) index++
        _state.value = _state.value.copy(index = index, positionMs = 0L)
        prepareCurrent(autoplay = true)
    }

    override fun prev() {
        if (queue.isEmpty()) return
        if (index > 0) index--
        _state.value = _state.value.copy(index = index, positionMs = 0L)
        prepareCurrent(autoplay = true)
    }

    override fun seekTo(positionMs: Long) {
        val player = mp ?: return

        val dur = player.duration.toLong().coerceAtLeast(1L)
        val target = positionMs.coerceIn(0L..dur)

        player.seekTo(target.toInt())
        _state.value = _state.value.copy(positionMs = target, durationMs = dur)
    }

    private fun prepareCurrent(autoplay: Boolean) {
        release()

        val track = queue.getOrNull(index) ?: return
        val rawName = track.uri.ifBlank { track.id }
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        require(resId != 0) { "Raw resource not found: res/raw/$rawName(.mp3)" }

        val created = MediaPlayer.create(context, resId)
        mp = created

        created.setOnCompletionListener {
            stopTicker()

            if (index < queue.lastIndex) {
                index++
                _state.value = _state.value.copy(index = index, isPlaying = true, positionMs = 0L)
                prepareCurrent(autoplay = true)
            } else {
                _state.value = _state.value.copy(isPlaying = false, positionMs = _state.value.durationMs)
            }
        }

        // Update duration immediately (safe once created)
        val dur = created.duration.takeIf { it > 0 }?.toLong() ?: 0L
        _state.value = _state.value.copy(durationMs = dur, positionMs = 0L)

        if (autoplay) {
            created.start()
            _state.value = _state.value.copy(isPlaying = true)
            startTicker()
        } else {
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return

        ticker = scope.launch {
            while (isActive) {
                delay(tickMs)

                val player = mp ?: continue
                if (!player.isPlaying) continue

                val pos = player.currentPosition.toLong()
                val dur = player.duration.takeIf { it > 0 }?.toLong() ?: _state.value.durationMs

                _state.value = _state.value.copy(
                    positionMs = pos.coerceAtMost(dur),
                    durationMs = dur
                )
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun release() {
        stopTicker()
        mp?.release()
        mp = null
    }

    fun dispose() {
        release()
        scope.cancel()
    }
}
