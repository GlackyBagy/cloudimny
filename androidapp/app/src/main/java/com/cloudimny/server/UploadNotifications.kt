package com.cloudimny.server

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

const val UPLOAD_NOTIFICATION_CHANNEL_ID = "track_upload"
const val UPLOAD_NOTIFICATION_ID = 1001

object UploadNotifications {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            UPLOAD_NOTIFICATION_CHANNEL_ID,
            "Track upload",
            NotificationManager.IMPORTANCE_LOW
        )
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
}
