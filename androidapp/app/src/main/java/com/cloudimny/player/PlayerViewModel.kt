package com.cloudimny.player

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cloudimny.models.meta.Track
import com.cloudimny.server.ServerRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var controller: MediaController? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentTrackFromController()
        }
    }

    init {
        viewModelScope.launch {
            val sessionToken =
                SessionToken(
                    getApplication(),
                    ComponentName(getApplication(), PlaybackService::class.java)
                )
            val mediaController = MediaController.Builder(getApplication(), sessionToken)
                .buildAsync()
                .await()

            controller = mediaController
            mediaController.addListener(playerListener)
            _isPlaying.value = mediaController.isPlaying
            updateCurrentTrackFromController()

            while (true) {
                _position.value = mediaController.currentPosition.coerceAtLeast(0)
                val duration = mediaController.duration
                _duration.value = if (duration == C.TIME_UNSET || duration < 0) 0L else duration
                delay(500.milliseconds)
            }
        }
    }

    private fun updateCurrentTrackFromController() {
        val mediaId = controller?.currentMediaItem?.mediaId ?: return
        val track = PlaybackQueue.tracks.value.firstOrNull { it.id?.toString() == mediaId }
        _currentTrack.value = track
        if (track != null) {
            PlaybackQueue.moveTo(track)
        }
    }

    fun play(tracks: List<Track>, track: Track) {
        val playable = tracks.filter { it.id != null }
        val startIndex = playable.indexOfFirst { it.id == track.id }
        if (startIndex == -1) return

        val ctl = controller ?: return
        PlaybackQueue.set(playable, startIndex)
        ctl.setMediaItems(playable.map { it.toMediaItem() }, startIndex, 0L)
        ctl.prepare()
        ctl.play()
    }

    fun playNext() {
        val ctl = controller ?: return
        if (ctl.hasNextMediaItem()) ctl.seekToNext()
    }

    fun playPrevious() {
        val ctl = controller ?: return
        if (ctl.hasPreviousMediaItem()) ctl.seekToPrevious()
    }

    fun togglePlayPause() {
        val ctl = controller ?: return
        if (ctl.playbackState == Player.STATE_IDLE) return
        if (ctl.playbackState == Player.STATE_ENDED) {
            ctl.seekTo(0)
            ctl.play()
            return
        }
        ctl.playWhenReady = !ctl.playWhenReady
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _position.value = positionMs
    }

    private fun Track.toMediaItem(): MediaItem {
        val trackId = checkNotNull(id)
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist?.nickname)
            .build()

        return MediaItem.Builder()
            .setMediaId(trackId.toString())
            .setUri(ServerRepository.streamingUrl(getApplication(), trackId))
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
        addListener({
            try {
                cont.resume(get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, MoreExecutors.directExecutor())
        cont.invokeOnCancellation { cancel(false) }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }
}
