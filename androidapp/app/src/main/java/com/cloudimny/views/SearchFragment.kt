package com.cloudimny.views

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.models.meta.Playlist
import com.cloudimny.models.meta.Track
import com.cloudimny.player.PlayerViewModel
import com.cloudimny.server.MetadataService
import com.cloudimny.views.home.PlaylistAdapter
import com.cloudimny.views.home.TrackAdapter
import com.cloudimny.views.playlist.PlaylistDetailFragment
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    private var allTracks: List<Track> = emptyList()
    private var allPlaylists: List<Playlist> = emptyList()

    private lateinit var tracksHeader: View
    private lateinit var tracksList: RecyclerView
    private lateinit var playlistsHeader: View
    private lateinit var playlistsList: RecyclerView
    private lateinit var emptyLabel: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.nav_search))

        val searchInput: EditText = view.findViewById(R.id.search_input)
        tracksHeader = view.findViewById(R.id.search_tracks_header)
        tracksList = view.findViewById<RecyclerView>(R.id.search_tracks_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        playlistsHeader = view.findViewById(R.id.search_playlists_header)
        playlistsList = view.findViewById<RecyclerView>(R.id.search_playlists_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        emptyLabel = view.findViewById(R.id.search_empty_label)

        searchInput.addTextChangedListener {
            applyFilter(it?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            allTracks = MetadataService.loadAllTracks(requireContext())
            allPlaylists = MetadataService.loadAllPlaylists(requireContext())
            applyFilter(searchInput.text.toString())
        }
    }

    private fun applyFilter(query: String) {
        val trimmed = query.trim()

        val matchingTracks = if (trimmed.isEmpty()) emptyList() else allTracks.filter { track ->
            track.title.orEmpty().contains(trimmed, ignoreCase = true) ||
                track.artist?.nickname.orEmpty().contains(trimmed, ignoreCase = true)
        }
        val matchingPlaylists = if (trimmed.isEmpty()) emptyList() else allPlaylists.filter { playlist ->
            playlist.name.orEmpty().contains(trimmed, ignoreCase = true)
        }

        tracksHeader.visibility = if (matchingTracks.isEmpty()) View.GONE else View.VISIBLE
        tracksList.adapter = TrackAdapter(matchingTracks) { track ->
            playerViewModel.play(matchingTracks, track)
        }

        playlistsHeader.visibility = if (matchingPlaylists.isEmpty()) View.GONE else View.VISIBLE
        playlistsList.adapter = PlaylistAdapter(matchingPlaylists) { playlist ->
            val playlistId = playlist.id ?: return@PlaylistAdapter
            parentFragmentManager.beginTransaction()
                .replace(R.id.main, PlaylistDetailFragment.newInstance(playlistId))
                .addToBackStack(null)
                .commit()
        }

        emptyLabel.visibility = if (trimmed.isNotEmpty() &&
            matchingTracks.isEmpty() &&
            matchingPlaylists.isEmpty()
        ) View.VISIBLE else View.GONE
    }
}
