package com.cloudimny.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.cloudimny.local.db.MetadataDbHelper
import com.cloudimny.models.meta.Artist
import com.cloudimny.models.meta.Playlist
import com.cloudimny.models.meta.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.core.database.sqlite.transaction

class LocalDbRepository private constructor(context: Context) {
    private val dbHelper = MetadataDbHelper.get(context)

    suspend fun getAllTracks(): List<Track> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            "tracks", null, null, null, null, null, null
        ).use { cursor -> cursor.toTrackList() }
    }

    suspend fun saveTracks(tracks: List<Track>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            tracks.forEach { insertTrack(db, it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun saveTrack(track: Track) = withContext(Dispatchers.IO) {
        insertTrack(dbHelper.writableDatabase, track)
    }

    suspend fun getAllPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        db.query("playlists", null, null, null, null, null, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow("id")))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    add(Playlist(id, name, getTracksForPlaylist(db, id)))
                }
            }
        }
    }

    suspend fun getPlaylist(id: UUID): Playlist? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        db.query("playlists", null, "id = ?", arrayOf(id.toString()), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            Playlist(id, name, getTracksForPlaylist(db, id))
        }
    }

    suspend fun savePlaylists(playlists: List<Playlist>) = withContext(Dispatchers.IO) {
        playlists.forEach { savePlaylistInternal(it) }
    }

    suspend fun savePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        savePlaylistInternal(playlist)
    }

    private fun savePlaylistInternal(playlist: Playlist) {
        val playlistId = playlist.id ?: return
        val db = dbHelper.writableDatabase
        db.transaction {
            try {
                insertWithOnConflict(
                    "playlists", null,
                    ContentValues().apply {
                        put("id", playlistId.toString())
                        put("name", playlist.name)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )

                playlist.songList.forEach { insertTrack(this, it) }

                delete("playlist_track_refs", "playlist_id = ?", arrayOf(playlistId.toString()))
                playlist.songList.forEachIndexed { index, track ->
                    val trackId = track.id ?: return@forEachIndexed
                    insertWithOnConflict(
                        "playlist_track_refs", null,
                        ContentValues().apply {
                            put("playlist_id", playlistId.toString())
                            put("track_id", trackId.toString())
                            put("position", index)
                        },
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }

            } finally {
            }
        }
    }

    private fun insertTrack(db: SQLiteDatabase, track: Track) {
        val trackId = track.id ?: return
        db.insertWithOnConflict(
            "tracks", null,
            ContentValues().apply {
                put("id", trackId.toString())
                put("title", track.title)
                put("artist_id", track.artist?.id?.toString())
                put("artist_nickname", track.artist?.nickname)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun getTracksForPlaylist(db: SQLiteDatabase, playlistId: UUID): List<Track> {
        val query = """
            SELECT tracks.* FROM tracks
            INNER JOIN playlist_track_refs ON tracks.id = playlist_track_refs.track_id
            WHERE playlist_track_refs.playlist_id = ?
            ORDER BY playlist_track_refs.position ASC
        """.trimIndent()

        return db.rawQuery(query, arrayOf(playlistId.toString())).use { cursor -> cursor.toTrackList() }
    }

    private fun Cursor.toTrackList(): List<Track> = buildList {
        while (moveToNext()) add(toTrack())
    }

    private fun Cursor.toTrack(): Track {
        val id = UUID.fromString(getString(getColumnIndexOrThrow("id")))
        val title = getString(getColumnIndexOrThrow("title"))
        val artistId = getString(getColumnIndexOrThrow("artist_id"))?.let(UUID::fromString)
        val artistNickname = getString(getColumnIndexOrThrow("artist_nickname"))
        val artist = if (artistId != null || artistNickname != null) Artist(artistId, artistNickname) else null
        return Track(id, title, artist)
    }

    companion object {
        @Volatile
        private var instance: LocalDbRepository? = null

        fun get(context: Context): LocalDbRepository =
            instance ?: synchronized(this) {
                instance ?: LocalDbRepository(context.applicationContext).also { instance = it }
            }
    }
}
