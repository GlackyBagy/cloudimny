package com.cloudimny.views.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.models.meta.Track
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracks = emptyList<Track>()//ServerRepository.loadAllTracks(requireContext())

            view.findViewById<RecyclerView>(R.id.all_tracks_list).apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = TrackAdapter(allTracks) { track ->
                    Toast.makeText(requireContext(), "Clicked: ${track.title}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}