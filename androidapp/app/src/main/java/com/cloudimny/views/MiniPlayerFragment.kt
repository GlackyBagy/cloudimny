package com.cloudimny.views

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.graphics.Bitmap
import com.cloudimny.R
import com.cloudimny.covers.CoverLoader
import com.cloudimny.mirror.MirrorController
import com.cloudimny.mirror.MirrorState
import com.cloudimny.player.PlayerViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

class MiniPlayerFragment : Fragment(R.layout.fragment_mini_player) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trackCover: ImageView = view.findViewById(R.id.track_cover)
        val trackTitle: TextView = view.findViewById(R.id.track_title)
        val trackArtist: TextView = view.findViewById(R.id.track_artist)
        val playButton: ImageButton = view.findViewById(R.id.play_button)

        view.visibility = View.GONE

        view.setOnClickListener {
            (requireActivity() as MainActivity).openPlayer()
        }

        playButton.setOnClickListener {
            playerViewModel.togglePlayPause()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // мини-плеер — единственный вход обратно в развёрнутый плеер, поэтому в режиме
                // зеркала он обязан быть виден и тогда, когда наш собственный плеер молчит:
                // в passthrough звучит источник, а currentTrack пуст
                launch {
                    combine(
                        playerViewModel.currentTrack,
                        MirrorController.state
                    ) { track, mirror -> track to mirror }
                        .collect { (track, mirror) ->
                            when (mirror) {
                                is MirrorState.Substituted -> show(
                                    view, trackTitle, trackArtist, trackCover,
                                    mirror.track.title, mirror.track.artist?.nickname,
                                    mirror.source.artwork, mirror.track.id
                                )

                                is MirrorState.Passthrough -> show(
                                    view, trackTitle, trackArtist, trackCover,
                                    mirror.source.title, mirror.source.artist,
                                    mirror.source.artwork
                                )

                                MirrorState.Waiting -> show(
                                    view, trackTitle, trackArtist, trackCover,
                                    getString(R.string.mirror_waiting), null, null
                                )

                                MirrorState.Off -> {
                                    view.visibility = if (track == null) View.GONE else View.VISIBLE
                                    trackTitle.text = track?.title
                                    trackArtist.text = track?.artist?.nickname
                                    CoverLoader.load(trackCover, track?.id)
                                }
                            }
                        }
                }

                launch {
                    playerViewModel.isPlaying.collect { isPlaying ->
                        playButton.setImageResource(
                            if (isPlaying) R.drawable.pause_icon else R.drawable.play_arrow_icon
                        )
                    }
                }
            }
        }
    }

    private fun show(
        root: View,
        titleView: TextView,
        artistView: TextView,
        coverView: ImageView,
        title: String?,
        artist: String?,
        artwork: Bitmap?,
        fallbackTrackId: UUID? = null
    ) {
        root.visibility = View.VISIBLE
        titleView.text = title
        artistView.text = artist

        if (artwork == null) {
            CoverLoader.load(coverView, fallbackTrackId)
            return
        }

        // снимаем возможный незавершённый запрос обложки: иначе он доедет позже
        // и перезапишет картинку, взятую из уведомления источника
        CoverLoader.load(coverView, null)
        coverView.setImageBitmap(artwork)
    }
}
