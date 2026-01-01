package com.example.sharpwave2026.library

import android.content.Context
import android.provider.MediaStore
import com.example.sharpwave2026.player.Track

class AndroidAudioLibrary(
    private val context: Context
): AudioLibrary {

    override suspend fun scanTracks(): List<Track> {
        val resolver = context.contentResolver

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val result = mutableListOf<Track>()

        resolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol) ?: "Unknown"
                val durationMs = cursor.getLong(durCol)

                val uri = android.content.ContentUris.withAppendedId(collection, id)

                result += Track(
                    id = uri.toString(),
                    title = title,
                    artist = artist,
                    durationMs = durationMs
                )

            }
        }

        return result
    }
}