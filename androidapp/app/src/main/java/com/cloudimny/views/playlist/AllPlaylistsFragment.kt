package com.cloudimny.views.playlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.server.ServerRepository
import com.cloudimny.views.MainActivity
import com.cloudimny.views.home.PlaylistAdapter
import kotlinx.coroutines.launch

class AllPlaylistsFragment : Fragment(R.layout.fragment_item_list) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.all_playlists))

        viewLifecycleOwner.lifecycleScope.launch {
            val allPlaylists = ServerRepository.loadAllPlaylists(requireContext())

            view.findViewById<RecyclerView>(R.id.items_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = PlaylistAdapter(allPlaylists) { playlist ->
                    val playlistId = playlist.id ?: return@PlaylistAdapter
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main, PlaylistDetailFragment.newInstance(playlistId))
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }
}
