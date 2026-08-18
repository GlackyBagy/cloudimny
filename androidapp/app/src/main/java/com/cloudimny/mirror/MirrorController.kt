package com.cloudimny.mirror

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cloudimny.models.matching.TrackMatcher
import com.cloudimny.models.meta.Track
import com.cloudimny.player.PlaybackQueue
import com.cloudimny.player.PlaybackService
import com.cloudimny.player.toMediaItem
import com.cloudimny.server.MetadataService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.minutes

sealed interface MirrorState {
    /** Mirroring is off; the player behaves as an ordinary player. */
    data object Off : MirrorState

    /** On, but nothing is playing in another app yet. */
    data object Waiting : MirrorState

    /** The library had this recording: the source is paused and we are playing our copy. */
    data class Substituted(val source: SourceTrack, val track: Track) : MirrorState

    /** The library did not have it, so the source was handed its audio back. */
    data class Passthrough(val source: SourceTrack) : MirrorState
}

/**
 * Plays the library's own copy of whatever another app is playing.
 *
 * The source is never left audible while a substitution is on: Android gives no way to mute one
 * foreign app, so the only way to avoid two songs at once is to pause it. That turns the source
 * into a cursor over its own queue — it holds the position, and skipping is done by sending it the
 * command rather than by moving a queue of ours, which is why [forward] exists at all.
 *
 * Application-scoped rather than tied to a screen: the point is to keep mirroring while the app is
 * in the background, which it does for as long as the playback service keeps the process alive.
 */
object MirrorController {
    private const val TAG = "MirrorController"
    private const val LOGGED_LIBRARY_LIMIT = 15
    private val SERVER_REFRESH_INTERVAL_MS = 2.minutes.inWholeMilliseconds

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastServerRefreshAt = 0L

    private var session: SourceSession? = null
    private var controller: MediaController? = null
    private var watcher: Job? = null
    private var library: List<Track> = emptyList()

    private val _state = MutableStateFlow<MirrorState>(MirrorState.Off)
    val state: StateFlow<MirrorState> = _state.asStateFlow()

    /**
     * The queue belongs to the source, and while a substitution plays the source is paused — so
     * nothing advances it on its own. Reaching the end of our copy is the same event as the user
     * asking for the next track, and it is answered the same way.
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return

            val substituted = _state.value as? MirrorState.Substituted ?: return
            // Сессия у плеера общая: пока зеркало включено, пользователь может завести обычный
            // трек из списка, и его окончание не должно листать чужую очередь. Двигаем источник,
            // только если доиграл именно тот трек, который мы подставили.
            if (controller?.currentMediaItem?.mediaId != substituted.track.id?.toString()) return

            Log.i(TAG, "substituted track finished, advancing the source")
            session?.send(SourceCommand.NEXT)
        }
    }

    val isActive: Boolean get() = _state.value != MirrorState.Off

    /**
     * @return false when notification access is missing — the caller has to send the user to
     * [SourceSession.accessSettingsIntent], as there is no runtime permission to ask for.
     */
    fun start(context: Context): Boolean {
        if (isActive) return true

        // Всё, что живёт дольше экрана, держит только application-контекст: этот объект переживает
        // и фрагмент, и активити, а корутина ниже удерживает переданный контекст до самого stop().
        val appContext = context.applicationContext

        val source = SourceSession(appContext)
        if (!source.start()) {
            Log.w(TAG, "not started: notification access is missing")
            return false
        }
        Log.i(TAG, "starting, loading library…")

        session = source
        _state.value = MirrorState.Waiting

        watcher = scope.launch {
            refreshLibrary(appContext)
            controller = runCatching { connectToPlayer(appContext) }
                .onFailure { Log.e(TAG, "no connection to the playback service", it) }
                .getOrNull()
                ?.also { it.addListener(playerListener) }
            Log.i(TAG, "started: library=${library.size}, player=${controller != null}")

            source.current.collectLatest { onSourceChanged(appContext, it) }
        }

        return true
    }

    fun stop() {
        watcher?.cancel()
        watcher = null

        // Источник оставляем играющим — пользователь выключил подмену, а не музыку. Но только
        // если мы его и остановили: в passthrough и до первого совпадения он играет сам, и
        // безусловный PLAY завёл бы чужое приложение, которое никто не просил.
        if (_state.value is MirrorState.Substituted) session?.send(SourceCommand.PLAY)
        session?.stop()
        session = null

        // слушателя снимаем до паузы: иначе он ещё может дёрнуть уже отпущенный источник
        controller?.removeListener(playerListener)
        controller?.pause()
        controller?.release()
        controller = null

        library = emptyList()
        _state.value = MirrorState.Off
    }

    /**
     * Routes a transport command according to who is making the sound. Skipping always goes to the
     * source — it owns the queue — while play and pause act on whichever side is actually audible.
     */
    fun forward(command: SourceCommand) {
        when (_state.value) {
            is MirrorState.Substituted -> when (command) {
                SourceCommand.NEXT, SourceCommand.PREVIOUS -> session?.send(command)
                // источник обязан молчать, пока звучит подменённый трек
                SourceCommand.PLAY, SourceCommand.PAUSE -> Unit
            }

            is MirrorState.Passthrough -> session?.send(command)

            MirrorState.Waiting, MirrorState.Off -> Unit
        }
    }

    /**
     * Position, duration and seeking of the source, for the stretch where it is the one making the
     * sound. Without these the player would be showing a silent local track's numbers next to a
     * song the user can hear, and the seek bar would sit at zero.
     */
    fun sourcePositionMs(): Long = session?.currentPositionMs() ?: 0L

    fun sourceDurationMs(): Long = session?.durationMs() ?: 0L

    fun seekSource(positionMs: Long) {
        session?.seekTo(positionMs)
    }

    private suspend fun onSourceChanged(context: Context, source: SourceTrack?) {
        if (source == null) {
            _state.value = MirrorState.Waiting
            return
        }

        // Пересопоставлять есть смысл только на смене записи, но снимок источника всё равно надо
        // обновить: пауза, позиция и обложка меняются в пределах одного трека, а на устаревшем
        // снимке кнопка play/pause считала бы источник стоящим и слала бы PLAY второй раз подряд.
        val known = _state.value
        if (known is MirrorState.Substituted && source.sameRecordingAs(known.source)) {
            _state.value = known.copy(source = source)
            return
        }
        if (known is MirrorState.Passthrough && source.sameRecordingAs(known.source)) {
            _state.value = known.copy(source = source)
            return
        }

        // перечитываем локальный кэш на каждой смене записи: он дёшев, а трек могли залить
        // уже после того, как зеркало включили
        refreshLibrary(context)
        var match = TrackMatcher.match(source.title, source.artist, library)

        // промах может значить и «нет у нас такого», и «наш список устарел» — второе проверяем
        if (match == null && mayAskServer()) {
            refreshLibrary(context, force = true)
            match = TrackMatcher.match(source.title, source.artist, library)
        }

        Log.i(
            TAG,
            "source='${source.artist} - ${source.title}', library=${library.size} -> " +
                    (match?.let { "'${it.artist?.nickname} - ${it.title}'" } ?: "NO MATCH")
        )
        if (match == null) logLibrary()

        if (match == null) {
            controller?.pause()
            session?.send(SourceCommand.PLAY)
            _state.value = MirrorState.Passthrough(source)
            return
        }

        session?.send(SourceCommand.PAUSE)
        play(context, match)
        _state.value = MirrorState.Substituted(source, match)
    }

    /**
     * @param force skips the local cache and asks the server. Reserved for the miss path: without
     * it a track uploaded from another device would stay invisible, since the cache is only
     * refreshed by a pull on one of the list screens.
     *
     * On failure the previous list is kept rather than cleared — a server that went away should
     * cost the tracks it has not told us about, not the ones already known.
     */
    /** On a miss, what was actually compared — the fastest way to tell a stale list from a bad score. */
    private fun logLibrary() {
        Log.i(
            TAG,
            "compared against: " + library.take(LOGGED_LIBRARY_LIMIT)
                .joinToString(" | ") { "'${it.artist?.nickname} - ${it.title}'" } +
                    if (library.size > LOGGED_LIBRARY_LIMIT) " …" else ""
        )
    }

    private suspend fun refreshLibrary(context: Context, force: Boolean = false) {
        library = runCatching { MetadataService.loadAllTracks(context, forceRefresh = force) }
            .onFailure { Log.e(TAG, "library unavailable", it) }
            .getOrDefault(library)
    }

    /**
     * Asking the server on every miss would mean a request per track for anyone whose library
     * covers only part of what they listen to, so misses only trigger one this often.
     */
    private fun mayAskServer(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastServerRefreshAt < SERVER_REFRESH_INTERVAL_MS) return false

        lastServerRefreshAt = now
        return true
    }

    private fun play(context: Context, track: Track) {
        val player = controller
        if (player == null) {
            Log.e(TAG, "matched '${track.title}' but the playback service never connected")
            return
        }

        // очередь из одного трека: дальше и назад ведёт источник, а не наш список
        PlaybackQueue.set(listOf(track), 0)
        player.setMediaItems(listOf(track.toMediaItem(context)), 0, 0L)
        player.prepare()
        player.play()
    }

    private suspend fun connectToPlayer(context: Context): MediaController {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        return MediaController.Builder(context, token).buildAsync().await()
    }

    private suspend fun <T> ListenableFuture<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addListener({
                try {
                    continuation.resume(get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, MoreExecutors.directExecutor())
            continuation.invokeOnCancellation { cancel(false) }
        }
}
