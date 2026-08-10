package com.cloudimny.covers

import android.widget.ImageView
import com.cloudimny.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Binds covers to views. Everything below the view lives in [CoverRepository]; this only decides
 * which bitmap a given [ImageView] currently owns.
 *
 * The bitmap is handed over unmasked: rounding it here would apply the radius in bitmap pixels,
 * which the view then scales down along with the image. The shape belongs to the view, where it is
 * stated once next to the placeholder it has to match — see `Widget.Cloudimny.Cover`.
 */
object CoverLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Shows the cover of [trackId] in [target], leaving the placeholder background visible until —
     * and unless — one arrives. Safe on recycled views: the request of the previous binding is
     * cancelled, so a slow cover cannot land on the row that has since taken the view over.
     */
    fun load(target: ImageView, trackId: UUID?) {
        (target.getTag(R.id.cover_request) as? Job)?.cancel()
        target.setTag(R.id.cover_request, null)
        target.setImageDrawable(null)

        if (trackId == null) return

        val cached = CoverRepository.cached(trackId)
        if (cached != null) {
            target.setImageBitmap(cached)
            return
        }

        target.setTag(R.id.cover_request, scope.launch {
            val cover = CoverRepository.cover(target.context, trackId) ?: return@launch
            target.setImageBitmap(cover)
        })
    }
}
