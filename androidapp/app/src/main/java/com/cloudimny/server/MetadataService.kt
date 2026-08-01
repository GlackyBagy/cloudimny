package com.cloudimny.server

import android.content.Context
import android.net.Uri
import com.cloudimny.local.LocalDbRepository
import com.cloudimny.models.meta.Playlist
import com.cloudimny.models.meta.Track
import java.util.UUID

object MetadataService {
    suspend fun loadAllTracks(context: Context, forceRefresh: Boolean = false): List<Track> {
        val localDb = LocalDbRepository.get(context)
        if (!forceRefresh) {
            val cached = localDb.getAllTracks()
            if (cached.isNotEmpty()) return cached
        }

        val tracks = ServerRepository.loadAllTracks(context)
        localDb.saveTracks(tracks)
        return tracks
    }

    suspend fun loadAllPlaylists(context: Context, forceRefresh: Boolean = false): List<Playlist> {
        val localDb = LocalDbRepository.get(context)
        if (!forceRefresh) {
            val cached = localDb.getAllPlaylists()
            if (cached.isNotEmpty()) return cached
        }

        val playlists = ServerRepository.loadAllPlaylists(context)
        localDb.savePlaylists(playlists)
        return playlists
    }

    suspend fun loadPlaylist(context: Context, id: UUID, forceRefresh: Boolean = false): Playlist {
        val localDb = LocalDbRepository.get(context)
        if (!forceRefresh) {
            localDb.getPlaylist(id)?.let { return it }
        }

        val playlist = ServerRepository.loadPlaylist(context, id)
        localDb.savePlaylist(playlist)
        return playlist
    }

    suspend fun createPlaylist(context: Context, name: String, trackIds: List<UUID>): Playlist {
        val playlist = ServerRepository.createPlaylist(context, name, trackIds)
        LocalDbRepository.get(context).savePlaylist(playlist)
        return playlist
    }

    suspend fun uploadTrack(context: Context, fileUri: Uri, title: String, artist: String): Track {
        val track = ServerRepository.uploadTrack(context, fileUri, title, artist)
        LocalDbRepository.get(context).saveTrack(track)
        return track
    }
}
