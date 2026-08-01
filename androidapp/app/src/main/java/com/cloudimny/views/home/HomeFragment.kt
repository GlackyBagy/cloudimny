package com.cloudimny.views.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cloudimny.R
import com.cloudimny.player.PlayerViewModel
import com.cloudimny.server.MetadataService
import com.cloudimny.views.AllTracksFragment
import com.cloudimny.views.MainActivity
import com.cloudimny.views.playlist.AllPlaylistsFragment
import com.cloudimny.views.playlist.PlaylistDetailFragment
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val PREVIEW_SIZE = 3

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tracksList: RecyclerView
    private lateinit var playlistsList: RecyclerView
    private lateinit var noPlaylistsLabel: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.app_name))

        view.findViewById<View>(R.id.all_tracks_arrow).setOnClickListener {
            openFragment(AllTracksFragment())
        }
        view.findViewById<View>(R.id.all_playlists_arrow).setOnClickListener {
            openFragment(AllPlaylistsFragment())
        }

        tracksList = view.findViewById<RecyclerView>(R.id.tracks_preview_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        playlistsList = view.findViewById<RecyclerView>(R.id.playlists_preview_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        noPlaylistsLabel = view.findViewById(R.id.no_playlists_label)

        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.secondary)
        swipeRefresh.setOnRefreshListener { loadContent(forceRefresh = true) }

        loadContent(forceRefresh = false)
    }

    private fun loadContent(forceRefresh: Boolean) {
        swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracksDeferred = async { MetadataService.loadAllTracks(requireContext(), forceRefresh) }
            val allPlaylistsDeferred = async { MetadataService.loadAllPlaylists(requireContext(), forceRefresh) }
            val allTracks = allTracksDeferred.await()
            val allPlaylists = allPlaylistsDeferred.await()

            tracksList.adapter = TrackAdapter(allTracks.take(PREVIEW_SIZE)) { track ->
                playerViewModel.play(allTracks, track)
            }

            noPlaylistsLabel.visibility = if (allPlaylists.isEmpty()) View.VISIBLE else View.GONE
            playlistsList.adapter = PlaylistAdapter(allPlaylists.take(PREVIEW_SIZE)) { playlist ->
                val playlistId = playlist.id ?: return@PlaylistAdapter
                openFragment(PlaylistDetailFragment.newInstance(playlistId))
            }

            swipeRefresh.isRefreshing = false
        }
    }

    private fun openFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()
    }
}
