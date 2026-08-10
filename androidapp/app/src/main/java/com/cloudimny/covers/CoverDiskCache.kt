package com.cloudimny.covers

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val CACHE_DIR_NAME = "cover_cache"
private const val CACHE_MAX_BYTES = 32L * 1024 * 1024
private const val CACHE_TRIM_TARGET_BYTES = CACHE_MAX_BYTES * 4 / 5

/**
 * Encoded covers on disk, one file per track, evicted least-recently-used — the image counterpart
 * of what [com.cloudimny.player.TrackCache] does for audio.
 */
internal object CoverDiskCache {
    private val trimLock = Mutex()

    suspend fun read(context: Context, trackId: UUID): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(cacheDir(context), trackId.toString())
        if (!file.exists()) return@withContext null

        try {
            val bytes = file.readBytes()
            // lastModified doubles as the LRU timestamp: touching it on every hit keeps the covers
            // that are actually shown ahead of those downloaded once and never looked at again
            file.setLastModified(System.currentTimeMillis())
            bytes
        } catch (e: IOException) {
            null
        }
    }

    suspend fun write(context: Context, trackId: UUID, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val dir = cacheDir(context)
            val target = File(dir, trackId.toString())
            // written aside and renamed: a download interrupted midway must not leave a truncated
            // file behind that every later read would decode into nothing
            val temp = File(dir, "$trackId.tmp")

            try {
                temp.writeBytes(bytes)
                if (!temp.renameTo(target)) temp.delete()
            } catch (e: IOException) {
                temp.delete()
                return@withContext
            }

            trim(dir)
        }
    }

    private suspend fun trim(dir: File) = trimLock.withLock {
        val files = dir.listFiles()?.sortedBy(File::lastModified) ?: return@withLock
        var total = files.sumOf(File::length)
        if (total <= CACHE_MAX_BYTES) return@withLock

        for (file in files) {
            if (total <= CACHE_TRIM_TARGET_BYTES) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
}
