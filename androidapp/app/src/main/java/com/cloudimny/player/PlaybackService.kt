package com.cloudimny.player

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.cloudimny.server.ServerRepository

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(TrackCache.get(this))
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(ServerRepository.httpClient(this)))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
