package com.cloudimny.views

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
import com.cloudimny.views.home.TrackAdapter
import kotlinx.coroutines.launch

class AllTracksFragment : Fragment(R.layout.fragment_item_list) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.all_tracks))

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracks = ServerRepository.loadAllTracks(requireContext())

            view.findViewById<RecyclerView>(R.id.items_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = TrackAdapter(allTracks) { track ->
                    playerViewModel.play(allTracks, track)
                }
            }
        }
    }
}
