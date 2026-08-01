package com.cloudimny.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "cloudimny_cache.db"
private const val DB_VERSION = 1

class MetadataDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tracks (
                id TEXT PRIMARY KEY,
                title TEXT,
                artist_id TEXT,
                artist_nickname TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE playlists (
                id TEXT PRIMARY KEY,
                name TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE playlist_track_refs (
                playlist_id TEXT NOT NULL,
                track_id TEXT NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (playlist_id, track_id)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_playlist_track_refs_playlist_id ON playlist_track_refs(playlist_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS playlist_track_refs")
        db.execSQL("DROP TABLE IF EXISTS playlists")
        db.execSQL("DROP TABLE IF EXISTS tracks")
        onCreate(db)
    }

    companion object {
        @Volatile
        private var instance: MetadataDbHelper? = null

        fun get(context: Context): MetadataDbHelper =
            instance ?: synchronized(this) {
                instance ?: MetadataDbHelper(context.applicationContext).also { instance = it }
            }
    }
}
