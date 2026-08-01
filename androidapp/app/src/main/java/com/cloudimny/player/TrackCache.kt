package com.cloudimny.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

private const val CACHE_DIR_NAME = "track_cache"
private const val CACHE_MAX_BYTES = 512L * 1024 * 1024

@UnstableApi
object TrackCache {
    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): SimpleCache =
        instance ?: synchronized(this) {
            instance ?: SimpleCache(
                File(context.applicationContext.cacheDir, CACHE_DIR_NAME),
                LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { instance = it }
        }
}
