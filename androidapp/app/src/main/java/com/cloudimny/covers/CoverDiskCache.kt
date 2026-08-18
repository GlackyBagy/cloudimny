package com.cloudimny.covers

import android.content.Context
import com.cloudimny.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val CACHE_DIR_NAME = "cover_cache"
private const val BYTES_IN_MEGABYTE = 1024L * 1024L

/**
 * Trimming down to the limit exactly would make the next write trim again; stopping below it buys
 * room for a run of writes between passes.
 */
private const val TRIM_TARGET_PERCENT = 80

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

            trim(dir, maxBytes(context))
        }
    }

    /**
     * Brings the cache under the limit now rather than at the next write, so lowering it in
     * settings frees the space there and then instead of whenever the next cover happens to arrive.
     */
    suspend fun trimToLimit(context: Context) = withContext(Dispatchers.IO) {
        trim(cacheDir(context), maxBytes(context))
    }

    private suspend fun trim(dir: File, maxBytes: Long) = trimLock.withLock {
        val files = dir.listFiles()?.sortedBy(File::lastModified) ?: return@withLock
        var total = files.sumOf(File::length)
        if (total <= maxBytes) return@withLock

        val target = maxBytes * TRIM_TARGET_PERCENT / 100
        for (file in files) {
            if (total <= target) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    private fun maxBytes(context: Context): Long =
        AppPreferences.getCoverCacheMb(context) * BYTES_IN_MEGABYTE

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
}
