package com.cloudimny.player

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.cloudimny.models.meta.Track
import com.cloudimny.server.ServerRepository

/**
 * Shared by the player and the mirror: both hand tracks to the same session, and a queue built two
 * different ways would drift in what the notification shows.
 */
fun Track.toMediaItem(context: Context): MediaItem {
    val trackId = checkNotNull(id)
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist?.nickname)
        // обложка уведомления и экрана блокировки грузится самим media3, мимо CoverRepository:
        // до бинда во View дело не доходит, а 404 без обложки он трактует как её отсутствие
        .setArtworkUri(ServerRepository.coverUrl(context, trackId).toUri())
        .build()

    return MediaItem.Builder()
        .setMediaId(trackId.toString())
        .setUri(ServerRepository.streamingUrl(context, trackId))
        .setMediaMetadata(metadata)
        .build()
}
