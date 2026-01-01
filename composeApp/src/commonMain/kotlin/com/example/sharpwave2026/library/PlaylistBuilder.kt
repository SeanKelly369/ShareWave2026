package com.example.sharpwave2026.library

import com.example.sharpwave2026.player.Track

object PlaylistBuilder {
    fun buildAuto(tracks: List<Track>): List<PlayList> {
        val clean = tracks.filter { it.uri != null }

        val all = PlayList("all", "All Tracks", clean)

        val audiobooks = PlayList(
            id = "audiobooks",
            name = "Audiobooks",
            tracks = clean.filter { (it.durationMs ?: 0L) >= 30L * 60L * 1000L }
        )

        val byArtist = clean
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .toList()
            .sortedBy { (artist, _) -> artist.lowercase() }
            .map { (artist, items) ->
                PlayList(
                    id = "artist:${artist.lowercase()}",
                    name = artist,
                    tracks = items
                )
            }

        return listOf(all, audiobooks) + byArtist
    }
}