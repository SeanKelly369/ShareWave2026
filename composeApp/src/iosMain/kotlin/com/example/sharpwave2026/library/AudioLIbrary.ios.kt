package com.example.sharpwave2026.library

import com.example.sharpwave2026.player.Track
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSDirectoryEnumerationSkipsHiddenFiles
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

actual fun provideAudioLibrary(): AudioLibrary = IosAudioLibrary()

private class IosAudioLibrary: AudioLibrary {

    override suspend fun scanTracks(): List<Track> = withContext(Dispatchers.Default) {
        val tracks = mutableListOf<Track>()

        tracks += scanDirectoryForMp3s(documentDirUrl())
        tracks += scanBundleForMp3s()

        tracks
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirUrl(): NSURL {
        val fm = NSFileManager.defaultManager
        val url = fm.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return requireNotNull(url) { "Could not resolve Documents directory URL" }
    }

    private fun scanDirectoryForMp3s(dir: NSURL): List<Track> {
        val fm = NSFileManager.defaultManager
        val enumerator = fm.enumeratorAtURL(
            url = dir,
            includingPropertiesForKeys = null,
            options = NSDirectoryEnumerationSkipsHiddenFiles,
            errorHandler = null
        ) ?: return emptyList()

        val out = mutableListOf<Track>()
        while (true) {
            val url = enumerator.nextObject() as? NSURL ?: break
            val path = url.path ?: continue
            if (!path.lowercase().endsWith(".mp3")) continue

            val name = (url.lastPathComponent ?: "Unknown").removeSuffix(".mp3")

            out += Track(
                id = name,
                title = name.replace('_', ' '),
                artist = "Unknown Artist",
                uri = path // Absolute path
            )
        }
        return out
    }

    private fun scanBundleForMp3s(): List<Track> {
        val urls = NSBundle.mainBundle.URLsForResourcesWithExtension("mp3", subdirectory = null)
            ?: return emptyList()

        return urls.mapNotNull { anyUrl ->
            val url = anyUrl as? NSURL ?: return@mapNotNull null
            val path = url.path ?: return@mapNotNull null
            val name = (url.lastPathComponent ?: "Unknown").removeSuffix(".mp3")

            Track(
                id = name,
                title = name.replace('_', ' '),
                artist = "Bundled",
                uri = path
            )
        }
    }
}