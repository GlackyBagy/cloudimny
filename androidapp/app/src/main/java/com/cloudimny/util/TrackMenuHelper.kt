package com.cloudimny.util

import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.cloudimny.R
import com.cloudimny.models.meta.Track
import com.cloudimny.server.ServerRepository
import com.cloudimny.views.upload.EditTrackFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TrackMenuHelper {
    companion object {
        fun showTrackOptionsMenu(
            anchor: View,
            track: Track,
            fragmentManager: FragmentManager,
            lifecycleScope: kotlinx.coroutines.CoroutineScope
        ) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.menu_track_meta, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_track_meta -> {
                        fragmentManager.beginTransaction()
                            .replace(R.id.main, EditTrackFragment(track))
                            .addToBackStack(null)
                            .commit()
                        true
                    }

                    R.id.action_delete_track -> {
                        MaterialAlertDialogBuilder(anchor.context)
                            .setTitle(R.string.delete_track_title)
                            .setMessage(
                                anchor.context.getString(R.string.delete_track_message)
                                    .format("${track.artist?.nickname} — ${track.title}")
                            )
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.delete_anyway) { _, _ ->
                                track.id?.let { id ->
                                    lifecycleScope.launch {
                                        anchor.context.runCatchingServerErrors {
                                            ServerRepository.deleteTrack(anchor.context, id)
                                            Toast.makeText(
                                                anchor.context,
                                                R.string.request_sent,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                            .show()
                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }
    }
}