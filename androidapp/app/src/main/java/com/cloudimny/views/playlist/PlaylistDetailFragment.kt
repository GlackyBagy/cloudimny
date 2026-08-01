package com.cloudimny.views.playlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.player.PlayerViewModel
import com.cloudimny.server.ServerRepository
import com.cloudimny.views.MainActivity
import com.cloudimny.views.home.TrackAdapter
import kotlinx.coroutines.launch
import java.util.UUID

class PlaylistDetailFragment : Fragment(R.layout.fragment_item_list) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playlistId = UUID.fromString(requireArguments().getString(ARG_PLAYLIST_ID))

        viewLifecycleOwner.lifecycleScope.launch {
            val playlist = ServerRepository.loadPlaylist(requireContext(), playlistId)
            (requireActivity() as MainActivity).setHeaderTitle(playlist.name.orEmpty())

            view.findViewById<RecyclerView>(R.id.items_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = TrackAdapter(playlist.songList) { track ->
                    playerViewModel.play(playlist.songList, track)
                }
            }
        }
    }

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: UUID): PlaylistDetailFragment = PlaylistDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_PLAYLIST_ID, playlistId.toString()) }
        }
    }
}
