package com.cloudimny.mirror

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.cloudimny.R
import com.cloudimny.views.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val ACTION_STOP = "com.cloudimny.mirror.STOP"

/**
 * Keeps mirroring alive while the app is away.
 *
 * [MirrorController] only lives as long as the process, and for most of a mirroring session there
 * is nothing keeping that process around: between substitutions our own player is paused, so no
 * foreground service of ours holds it and the system is free to reclaim us — mirroring would then
 * stop with nothing to show that it had. This service is the missing anchor.
 *
 * The notification it is obliged to post earns its place twice over: it says which mode mirroring
 * is in, and it carries the stop button — until now switching mirroring off meant a long press on
 * a tab bar icon, which nothing advertised.
 */
class MirrorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // до всего остального: на это система даёт считанные секунды после startForegroundService
        goForeground(buildNotification(MirrorState.Waiting))

        if (!MirrorController.start(applicationContext)) {
            stopSelf()
            return START_NOT_STICKY
        }

        // onStartCommand приходит и на повторный старт — двойная подписка слала бы
        // по два уведомления на каждую смену состояния
        if (watcher == null) {
            watcher = scope.launch {
                MirrorController.state.collect { state ->
                    if (state != MirrorState.Off) notify(state)
                }
            }
        }

        // перезапускать нечего: пересозданный сервис получил бы null-intent и пустое состояние,
        // а доступ к чужой сессии всё равно подтверждается заново
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        MirrorController.stop()
        super.onDestroy()
    }

    /**
     * `specialUse` rather than `mediaPlayback`: from Android 14 the media type is only granted to a
     * service that is actually playing something, and this one spends the passthrough stretches
     * deliberately silent.
     */
    private fun goForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                MIRROR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(MIRROR_NOTIFICATION_ID, notification)
        }
    }

    private fun notify(state: MirrorState) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        getSystemService<NotificationManager>()
            ?.notify(MIRROR_NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: MirrorState): Notification {
        val title: String
        val text: String?

        when (state) {
            is MirrorState.Substituted -> {
                title = getString(R.string.mirror_notification_substituted)
                text = describe(state.track.artist?.nickname, state.track.title)
            }

            is MirrorState.Passthrough -> {
                title = getString(R.string.mirror_notification_passthrough)
                text = describe(state.source.artist, state.source.title)
            }

            MirrorState.Waiting, MirrorState.Off -> {
                title = getString(R.string.mirror_notification_waiting)
                text = null
            }
        }

        return NotificationCompat.Builder(this, MIRROR_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.mirror_icon)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(
                R.drawable.mirror_icon,
                getString(R.string.mirror_notification_stop),
                stopIntent()
            )
            .build()
    }

    /** Neither half is guaranteed to be tagged, and "null — null" is worse than no subtitle at all. */
    private fun describe(artist: String?, title: String?): String? =
        listOfNotNull(artist?.takeIf { it.isNotBlank() }, title?.takeIf { it.isNotBlank() })
            .joinToString(" — ")
            .takeIf { it.isNotEmpty() }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        // NEW_TASK обязателен: активити запускает система от нашего имени, вне контекста активити
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this,
        1,
        Intent(this, MirrorService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MirrorService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MirrorService::class.java))
        }
    }
}
