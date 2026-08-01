package com.cloudimny.views.home

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
import com.cloudimny.views.AllTracksFragment
import com.cloudimny.views.MainActivity
import com.cloudimny.views.playlist.AllPlaylistsFragment
import com.cloudimny.views.playlist.PlaylistDetailFragment
import kotlinx.coroutines.launch

private const val PREVIEW_SIZE = 3

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.app_name))

        view.findViewById<View>(R.id.all_tracks_arrow).setOnClickListener {
            openFragment(AllTracksFragment())
        }
        view.findViewById<View>(R.id.all_playlists_arrow).setOnClickListener {
            openFragment(AllPlaylistsFragment())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracks = ServerRepository.loadAllTracks(requireContext())

            view.findViewById<RecyclerView>(R.id.tracks_preview_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = TrackAdapter(allTracks.take(PREVIEW_SIZE)) { track ->
                    playerViewModel.play(allTracks, track)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val allPlaylists = ServerRepository.loadAllPlaylists(requireContext())

            view.findViewById<View>(R.id.no_playlists_label).visibility =
                if (allPlaylists.isEmpty()) View.VISIBLE else View.GONE

            view.findViewById<RecyclerView>(R.id.playlists_preview_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = PlaylistAdapter(allPlaylists.take(PREVIEW_SIZE)) { playlist ->
                    val playlistId = playlist.id ?: return@PlaylistAdapter
                    openFragment(PlaylistDetailFragment.newInstance(playlistId))
                }
            }
        }
    }

    private fun openFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    }
}
