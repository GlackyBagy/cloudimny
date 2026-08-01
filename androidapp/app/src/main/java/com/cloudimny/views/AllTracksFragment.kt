package com.cloudimny.views

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
import com.cloudimny.views.home.TrackAdapter
import kotlinx.coroutines.launch

class AllTracksFragment : Fragment(R.layout.fragment_item_list) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.all_tracks))

        val itemsList: RecyclerView = view.findViewById<RecyclerView>(R.id.items_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        val swipeRefresh: SwipeRefreshLayout = view.findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.secondary)
        swipeRefresh.setOnRefreshListener { loadTracks(itemsList, swipeRefresh, forceRefresh = true) }

        loadTracks(itemsList, swipeRefresh, forceRefresh = false)
    }

    private fun loadTracks(itemsList: RecyclerView, swipeRefresh: SwipeRefreshLayout, forceRefresh: Boolean) {
        swipeRefresh.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracks = MetadataService.loadAllTracks(requireContext(), forceRefresh)
            itemsList.adapter = TrackAdapter(allTracks) { track ->
                playerViewModel.play(allTracks, track)
            }
            swipeRefresh.isRefreshing = false
        }
    }
}
