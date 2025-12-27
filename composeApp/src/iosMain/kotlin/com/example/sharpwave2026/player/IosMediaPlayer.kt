package com.example.sharpwave2026.player

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosMediaPlayer: Player {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)
    private var ticker: Job? = null
    private val tickMs = 200L

    private var ap: AVAudioPlayer? = null;
    private var queue: List<Track> = emptyList()
    private var index: Int = -1;

    private val _state = MutableStateFlow(PlayerState())
    override val state: Flow<PlayerState> = _state.asStateFlow()

    private val delegate = object: NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            stopTicker()

            // auto-advance like Android
            if (index < queue.lastIndex) {
                index++
                _state.value = _state.value.copy(index = index, isPlaying = true, positionMs = 0L)
                prepareCurrent(autoplay = true)
            } else {
                _state.value = _state.value.copy(isPlaying = false, positionMs = _state.value.durationMs)
            }
        }
    }

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

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

        val player = ap
        if (player == null) {
            prepareCurrent(autoplay = true)
            return
        }

        player.play()
        _state.value = _state.value.copy(isPlaying = true)
        startTicker()
    }

    override fun pause() {
        ap?.pause()
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
        val player = ap ?: return

        val durMs = (player.duration * 1000.0).toLong().coerceAtLeast(1L)
        val targetMs = positionMs.coerceIn(0L, durMs)

        player.currentTime = targetMs / 1000.0
        _state.value = _state.value.copy(positionMs = targetMs, durationMs = durMs)

        // if it is playing, keep the slider moving
        if (ap?.playing == true) startTicker()
    }

    private fun prepareCurrent(autoplay: Boolean) {
        release()

        val track = queue.getOrNull(index) ?: return
        val name = track.uri.ifBlank { track.id }

        val url = NSBundle.mainBundle.URLForResource(name, withExtension = "mp3")
            ?: error("Track not found in iOS bundle: $name.mp3")

        memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            val player = AVAudioPlayer(contentsOfURL = url, error = err.ptr)
                ?: error("AVAudioPlayer failed: ${err.value?.localizedDescription ?: "unknown"}")

            player.delegate = delegate
            player.prepareToPlay()
            ap = player

            val durMs = (player.duration * 1000.0).toLong().coerceAtLeast(0L)
            _state.value = _state.value.copy(durationMs = durMs, positionMs = 0L)
        }

        if (autoplay) {
            ap?.play()
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

                val player = ap ?: continue
                if (!player.playing) continue

                val posMs = (player.currentTime * 1000.0).toLong()
                val durMs = (player.duration * 1000.0).toLong().coerceAtLeast(1L)

                _state.value = _state.value.copy(
                    positionMs = posMs.coerceAtMost(durMs),
                    durationMs = durMs
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
        ap?.stop()
        ap = null
    }

    fun dispose() {
        release()
        job.cancel()
    }
}