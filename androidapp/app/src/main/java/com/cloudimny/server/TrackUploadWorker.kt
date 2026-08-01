package com.cloudimny.server

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cloudimny.R

class TrackUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val artist = inputData.getString(KEY_ARTIST) ?: return Result.failure()

        setForeground(createForegroundInfo(title))

        return try {
            MetadataService.uploadTrack(applicationContext, uri, title, artist)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val notification: Notification =
            NotificationCompat.Builder(applicationContext, UPLOAD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(applicationContext.getString(R.string.uploading_track_title, title))
                .setSmallIcon(R.drawable.add_icon)
                .setOngoing(true)
                .setProgress(0, 0, true)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                UPLOAD_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(UPLOAD_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
    }
}
