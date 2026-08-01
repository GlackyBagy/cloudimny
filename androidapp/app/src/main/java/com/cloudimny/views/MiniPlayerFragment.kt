package com.cloudimny.views

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cloudimny.R
import com.cloudimny.player.PlayerViewModel
import kotlinx.coroutines.launch

class MiniPlayerFragment : Fragment(R.layout.fragment_mini_player) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                launch {
                    playerViewModel.currentTrack.collect { track ->
                        view.visibility = if (track == null) View.GONE else View.VISIBLE
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
            }
        }
    }
}
