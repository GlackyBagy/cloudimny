package com.cloudimny.covers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.cloudimny.server.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

private const val MAX_COVER_SIZE_PX = 512
private const val MIN_MEMORY_CACHE_KB = 4 * 1024

/**
 * The server resolves covers asynchronously after an upload, so a miss is never final — it is only
 * re-checked no more often than this.
 */
private val MISS_RETRY_INTERVAL_MS = 5.minutes.inWholeMilliseconds

/**
 * Covers, three levels deep: decoded bitmaps in memory, encoded bytes on disk, the server last.
 * Requests for the same track collapse into one, so a list that shows the same track twice — or a
 * row rebound while its cover is still in flight — downloads it once.
 */
object CoverRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val memoryCache = object : LruCache<UUID, Bitmap>(memoryCacheSizeKb()) {
        override fun sizeOf(key: UUID, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    private val misses = ConcurrentHashMap<UUID, Long>()
    private val inFlight = mutableMapOf<UUID, Deferred<Bitmap?>>()
    private val inFlightLock = Mutex()

    /**
     * The bitmap already in memory, if any. Lets a caller fill a view without a coroutine at all,
     * which is what keeps a scrolling list from flickering through its placeholders.
     */
    fun cached(trackId: UUID): Bitmap? = memoryCache.get(trackId)

    suspend fun cover(context: Context, trackId: UUID): Bitmap? {
        memoryCache.get(trackId)?.let { return it }

        val appContext = context.applicationContext
        val request = inFlightLock.withLock {
            inFlight[trackId] ?: startLoad(appContext, trackId).also { inFlight[trackId] = it }
        }
        return request.await()
    }

    /**
     * Runs on the repository scope rather than the caller's: a row scrolled off screen cancels its
     * own wait, but the download it started keeps going and lands in the cache for the next bind.
     */
    private fun startLoad(context: Context, trackId: UUID): Deferred<Bitmap?> =
        scope.async {
            try {
                load(context, trackId)
            } finally {
                inFlightLock.withLock { inFlight.remove(trackId) }
            }
        }

    private suspend fun load(context: Context, trackId: UUID): Bitmap? {
        CoverDiskCache.read(context, trackId)?.let { cached ->
            decode(cached)?.let { bitmap ->
                memoryCache.put(trackId, bitmap)
                return bitmap
            }
        }

        if (missedRecently(trackId)) return null

        val bitmap = download(context, trackId)?.let { bytes ->
            decode(bytes)?.also { CoverDiskCache.write(context, trackId, bytes) }
        }

        if (bitmap == null) {
            misses[trackId] = System.currentTimeMillis()
            return null
        }

        misses.remove(trackId)
        memoryCache.put(trackId, bitmap)
        return bitmap
    }

    private fun missedRecently(trackId: UUID): Boolean {
        val missedAt = misses[trackId] ?: return false
        return System.currentTimeMillis() - missedAt < MISS_RETRY_INTERVAL_MS
    }

    /**
     * A track without a cover is an ordinary outcome, not a failure: the server answers 404 for one
     * it never matched to a release, and that reads the same here as an unreachable server.
     */
    private suspend fun download(context: Context, trackId: UUID): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                // внутри try вместе с запросом: пока сервер не настроен, URL вообще не построить
                val request = Request.Builder()
                    .url(ServerRepository.coverUrl(context, trackId))
                    .build()

                ServerRepository.httpClient(context).newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.bytes() else null
                }
            } catch (e: IOException) {
                null
            } catch (e: IllegalStateException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    /**
     * Covers arrive as Cover Art Archive thumbnails, already close to [MAX_COVER_SIZE_PX], but the
     * bound is worth keeping: a full-size scan decoded verbatim would cost tens of megabytes.
     */
    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var largest = maxOf(width, height)
        var sampleSize = 1
        while (largest / 2 >= MAX_COVER_SIZE_PX) {
            largest /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun memoryCacheSizeKb(): Int =
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(MIN_MEMORY_CACHE_KB)
}
