package com.cloudimny.mirror

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

const val MIRROR_NOTIFICATION_CHANNEL_ID = "mirror"
const val MIRROR_NOTIFICATION_ID = 1002

object MirrorNotifications {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            MIRROR_NOTIFICATION_CHANNEL_ID,
            "Mirroring",
            // тихо и без всплытия: уведомление здесь — индикатор и выключатель,
            // а не повод отвлечь на каждой смене трека
            NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
}
