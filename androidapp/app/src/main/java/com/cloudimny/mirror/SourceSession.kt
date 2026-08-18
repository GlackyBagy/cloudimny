package com.cloudimny.mirror

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MirrorController"

/** Rare enough to be free, frequent enough that a dropped callback is not felt. */
private const val RECONCILE_INTERVAL_MS = 3_000L

/** What the foreign player is showing right now. */
data class SourceTrack(
    val title: String?,
    val artist: String?,
    val artwork: Bitmap?,
    val isPlaying: Boolean
) {
    /** Two sessions describing the same recording differ only by state, which must not retrigger a match. */
    fun sameRecordingAs(other: SourceTrack?): Boolean =
        other != null && title == other.title && artist == other.artist
}

/** Commands forwarded to the source so it stays the cursor over the queue we are mirroring. */
enum class SourceCommand { PLAY, PAUSE, NEXT, PREVIOUS }

/**
 * A read/write handle on whichever other app is currently playing.
 *
 * Ours is excluded by package name: our own playback also registers a session, and mirroring it
 * would feed the matcher its own output.
 */
class SourceSession(context: Context) {
    private val appContext = context.applicationContext
    private val listenerComponent =
        ComponentName(appContext, MirrorNotificationListener::class.java)
    private val sessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val handler = Handler(Looper.getMainLooper())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _current = MutableStateFlow<SourceTrack?>(null)
    val current: StateFlow<SourceTrack?> = _current.asStateFlow()

    private var controller: MediaController? = null
    private var reconciler: Job? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // Сессия, которая только что заиграла, важнее той, за которой мы следим, — а узнать о
            // ней больше неоткуда: набор сессий при смене состояния не меняется, и слушатель
            // набора молчит. Пересматриваем выбор, как только наша перестала быть играющей.
            if (state?.state != PlaybackState.STATE_PLAYING) selectController()
            publish()
        }

        override fun onSessionDestroyed() {
            detachController()
            selectController()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { selectController() }

    /**
     * @return false when access is missing, in which case nothing is observed and the caller should
     * send the user to [accessSettingsIntent] instead.
     */
    fun start(): Boolean {
        if (!hasAccess(appContext)) return false

        return try {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                listenerComponent,
                handler
            )
            selectController()
            startReconciling()
            true
        } catch (_: SecurityException) {
            // доступ могли отозвать между проверкой и подпиской
            false
        }
    }

    fun stop() {
        // каждый start() заводит новый SourceSession, так что областью надо распоряжаться целиком,
        // иначе за сеанс их накапливается по одной на каждое включение
        scope.cancel()
        reconciler = null
        runCatching {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        }
        detachController()
        _current.value = null
    }

    /**
     * A slow safety net under the callbacks, not a replacement for them: third-party players are
     * not obliged to report every change, and a missed one would otherwise be lost for good —
     * mirroring would simply sit on a stale track with no way to notice. Re-reading a handful of
     * fields this rarely costs nothing; the callbacks still carry everything that arrives on time.
     */
    private fun startReconciling() {
        reconciler = scope.launch {
            while (true) {
                delay(RECONCILE_INTERVAL_MS)
                selectController()
                publish()
            }
        }
    }

    fun send(command: SourceCommand) {
        val transport = controller?.transportControls ?: return
        when (command) {
            SourceCommand.PLAY -> transport.play()
            SourceCommand.PAUSE -> transport.pause()
            SourceCommand.NEXT -> transport.skipToNext()
            SourceCommand.PREVIOUS -> transport.skipToPrevious()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.transportControls?.seekTo(positionMs)
    }

    /**
     * [PlaybackState.getPosition] is a reading taken at [PlaybackState.getLastPositionUpdateTime]
     * and is not refreshed while playing, so a caller polling it would see a frozen number. What is
     * returned here is that reading carried forward to now.
     */
    fun currentPositionMs(): Long {
        val state = controller?.playbackState ?: return 0L
        if (state.state != PlaybackState.STATE_PLAYING) return state.position.coerceAtLeast(0L)

        val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
        return (state.position + elapsed * state.playbackSpeed).toLong().coerceAtLeast(0L)
    }

    fun durationMs(): Long =
        controller?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L

    private fun selectController() {
        val candidates = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            emptyList()
        }

        val foreign = candidates.filter { it.packageName != appContext.packageName }
        // играющая сессия важнее просто существующей: их может быть несколько,
        // но озвучивает очередь всегда одна
        val next = foreign.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: foreign.firstOrNull()

        if (next?.sessionToken == controller?.sessionToken) return

        Log.i(TAG, "following ${next?.packageName ?: "nothing"}, ${foreign.size} foreign session(s)")

        detachController()
        controller = next?.also { it.registerCallback(controllerCallback, handler) }
        publish()
    }

    private fun detachController() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
    }

    companion object {
        /**
         * Whether the user has granted this app notification access. There is no runtime permission
         * for it — the grant lives in a secure setting the user edits in system settings, so it is
         * read rather than requested.
         */
        fun hasAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false

            return enabled.split(":")
                .mapNotNull { ComponentName.unflattenFromString(it) }
                .any { it.packageName == context.packageName }
        }

        /** Intent that takes the user to the screen where that grant is made. */
        fun accessSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    private fun publish() {
        val active = controller
        if (active == null) {
            _current.value = null
            return
        }

        val metadata = active.metadata
        _current.value = SourceTrack(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING
        )
    }
}
