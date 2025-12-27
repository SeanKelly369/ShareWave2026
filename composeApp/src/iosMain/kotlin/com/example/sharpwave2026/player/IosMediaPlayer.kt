package com.example.sharpwave2026.player

import kotlinx.cinterop.ptr
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private var ap: AVAudioPlayer? = null;
    private var queue: List<Track> = emptyList()
    private var index: Int = -1;

    private val _state = MutableStateFlow(PlayerState())
    override val state: Flow<PlayerState> = _state.asStateFlow()

    private val delegate = object: NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            // auto-advance like Android
            if (index < queue.lastIndex) {
                index++
                _state.value = _state.value.copy(index = index, isPlaying = true)
                prepareCurrent(autoplay = true)
            } else {
                _state.value = _state.value.copy(isPlaying = false)
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
        index = startIndex.coerceIn(tracks.indices)
        _state.value = PlayerState(queue = queue, index = index, isPlaying = false)
        prepareCurrent(autoplay = false)
    }

    override fun play() {
        if (queue.isEmpty() || index !in queue.indices) return
        if (ap == null) {
            prepareCurrent(autoplay = true)
            return
        }
        ap?.play()
        _state.value = _state.value.copy(isPlaying = true)
    }

    override fun pause() {
        ap?.pause()
        ap?.currentTime = ap?.currentTime ?: 0.0
        _state.value = _state.value.copy(isPlaying = false)
    }

    override fun toggle() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun next() {
        if (queue.isEmpty()) return
        if (index < queue.lastIndex) index++
        _state.value = _state.value.copy(index = index)
        prepareCurrent(autoplay = true)
    }

    override fun prev() {
        if (queue.isEmpty()) return
        if (index > 0) index--
        _state.value = _state.value.copy(index = index)
        prepareCurrent(autoplay = true)
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
        }

        if (autoplay) {
            ap?.play()
            _state.value = _state.value.copy(isPlaying = true)
        } else {
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    private fun release() {
        ap?.stop()
        ap = null
    }
}