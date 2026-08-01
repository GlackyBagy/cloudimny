package com.cloudimny.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cloudimny.models.meta.Track
import com.cloudimny.server.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val player = ExoPlayer.Builder(application).build()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                _isPlaying.value = false
            }
        })
    }

    fun play(track: Track) {
        val trackId = track.id ?: return

        if (_currentTrack.value?.id == trackId) {
            togglePlayPause()
            return
        }

        _currentTrack.value = track
        val url = ServerRepository.streamingUrl(getApplication<Application>(), trackId)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_IDLE) return
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.playWhenReady = true
            return
        }
        player.playWhenReady = !player.playWhenReady
    }

    override fun onCleared() {
        player.release()
    }
}
