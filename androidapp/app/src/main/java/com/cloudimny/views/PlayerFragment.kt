package com.cloudimny.views

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cloudimny.R
import com.cloudimny.player.PlaybackQueue
import com.cloudimny.player.PlayerViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PlayerFragment : Fragment(R.layout.fragment_player) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = statusBars.top)
            insets
        }

        val trackTitle: TextView = view.findViewById(R.id.track_title)
        val trackArtist: TextView = view.findViewById(R.id.track_artist)
        val playButton: ImageButton = view.findViewById(R.id.play_button)
        val previousButton: ImageButton = view.findViewById(R.id.previous_button)
        val nextButton: ImageButton = view.findViewById(R.id.next_button)
        val closeButton: ImageButton = view.findViewById(R.id.close_button)

        closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        playButton.setOnClickListener {
            playerViewModel.togglePlayPause()
        }

        previousButton.setOnClickListener {
            playerViewModel.playPrevious()
        }

        nextButton.setOnClickListener {
            playerViewModel.playNext()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    playerViewModel.currentTrack.collect { track ->
                        trackTitle.text = track?.title
                        trackArtist.text = track?.artist?.nickname
                    }
                }

                launch {
                    playerViewModel.isPlaying.collect { isPlaying ->
                        playButton.setImageResource(
                            if (isPlaying) R.drawable.pause_icon else R.drawable.play_arrow_icon
                        )
                    }
                }

                launch {
                    combine(PlaybackQueue.tracks, PlaybackQueue.currentIndex) { _, _ ->
                        PlaybackQueue.hasPrevious() to PlaybackQueue.hasNext()
                    }.collect { (hasPrevious, hasNext) ->
                        previousButton.isEnabled = hasPrevious
                        previousButton.alpha = if (hasPrevious) 1f else 0.3f
                        nextButton.isEnabled = hasNext
                        nextButton.alpha = if (hasNext) 1f else 0.3f
                    }
                }
            }
        }
    }
}
