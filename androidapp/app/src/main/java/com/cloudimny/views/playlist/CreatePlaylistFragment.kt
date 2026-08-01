package com.cloudimny.views.playlist

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.server.MetadataService
import com.cloudimny.views.MainActivity
import kotlinx.coroutines.launch

class CreatePlaylistFragment : Fragment(R.layout.fragment_create_playlist) {
    private var tracksAdapter: SelectableTrackAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.create_playlist_title))

        val nameInput: EditText = view.findViewById(R.id.playlist_name_input)
        val createButton: Button = view.findViewById(R.id.create_playlist_button)
        val tracksList: RecyclerView = view.findViewById(R.id.selectable_tracks_list)

        nameInput.addTextChangedListener {
            createButton.isEnabled = !it.isNullOrBlank()
        }

        tracksList.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val allTracks = MetadataService.loadAllTracks(requireContext())
            val adapter = SelectableTrackAdapter(allTracks)
            tracksAdapter = adapter
            tracksList.adapter = adapter
        }

        createButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener

            val selectedTrackIds = tracksAdapter?.selectedTrackIds?.toList().orEmpty()
            createButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                MetadataService.createPlaylist(requireContext(), name, selectedTrackIds)
                parentFragmentManager.popBackStack()
            }
        }
    }
}
