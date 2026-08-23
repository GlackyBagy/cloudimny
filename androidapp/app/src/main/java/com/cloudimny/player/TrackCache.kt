package com.cloudimny.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.cloudimny.AppPreferences
import java.io.File

private const val CACHE_DIR_NAME = "track_cache"
private const val BYTES_IN_MEGABYTE = 1024L * 1024L

/**
 * The budget is read once, when the cache is first opened: [SimpleCache] fixes its evictor at
 * construction and holds a lock on the directory for as long as it lives, so a size changed in
 * settings takes effect the next time the process starts rather than immediately.
 */
@UnstableApi
object TrackCache {
    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): SimpleCache =
        instance ?: synchronized(this) {
            instance ?: SimpleCache(
                File(context.applicationContext.cacheDir, CACHE_DIR_NAME),
                LeastRecentlyUsedCacheEvictor(
                    AppPreferences.getTrackCacheMb(context) * BYTES_IN_MEGABYTE
                ),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { instance = it }
        }

    /** [streamUrl] is the cache key: media3's CacheDataSource falls back to the request URI when
     * no custom key is set, and nothing in [toMediaItem] sets one. */
    fun remove(context: Context, streamUrl: String) {
        get(context).removeResource(streamUrl)
    }
}
