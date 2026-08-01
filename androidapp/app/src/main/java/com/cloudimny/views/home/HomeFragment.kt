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
import com.cloudimny.views.MainActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.app_name))

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracks = ServerRepository.loadAllTracks(requireContext())

            view.findViewById<RecyclerView>(R.id.all_tracks_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = TrackAdapter(allTracks) { track ->
                    playerViewModel.play(allTracks, track)
                }
            }
        }
    }
}