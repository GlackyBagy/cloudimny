package com.cloudimny.views.upload

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cloudimny.R
import com.cloudimny.models.meta.Artist
import com.cloudimny.models.meta.Track
import com.cloudimny.server.MetadataService
import com.cloudimny.server.ServerRepository
import com.cloudimny.util.runCatchingServerErrors
import com.cloudimny.views.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Objects

class EditTrackFragment(trackToEdit: Track) : Fragment(R.layout.fragment_upload_track) {
    private lateinit var titleInput: EditText
    private lateinit var artistInput: EditText
    private lateinit var saveButton: Button

    private val track: Track = trackToEdit // track isn't seen if used directly from constructor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.edit_track_title))

        view.findViewById<View>(R.id.choose_file_button)?.visibility = View.GONE
        view.findViewById<View>(R.id.chosen_file_name)?.visibility = View.GONE

        titleInput = view.findViewById(R.id.track_title_input)
        artistInput = view.findViewById(R.id.track_artist_input)
        saveButton = view.findViewById(R.id.upload_button)

        saveButton.setText(R.string.save_button_title)

        titleInput.setText(track.title)
        artistInput.setText(track.artist?.nickname ?: "")

        titleInput.addTextChangedListener(textWatcher)
        artistInput.addTextChangedListener(textWatcher)

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val nickname = artistInput.text.toString().trim()
            if (title.isEmpty() || nickname.isEmpty()) return@setOnClickListener

            if (nickname == track.artist?.nickname) {
                save(title, nickname, renameArtist = false)
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.artist_rename_strategy_title)
                .setMessage(R.string.artist_rename_strategy_message)
                .setNeutralButton(android.R.string.cancel, null)
                .setNegativeButton(R.string.artist_rename_all_tracks_strategy) { _, _ ->
                    save(title, nickname, renameArtist = true)
                }
                .setPositiveButton(R.string.artist_rename_single_track_strategy) { _, _ ->
                    save(title, nickname, renameArtist = false)
                }
                .show()
        }
    }

    private fun save(title: String, nickname: String, renameArtist: Boolean) {
        saveButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            var saved = false
            runCatchingServerErrors {
                val context = requireContext()

                if (renameArtist) {
                    track.artist?.id?.let {
                        ServerRepository.editArtist(context, Artist(it, nickname))
                    }
                }

                ServerRepository.editTrack(
                    context,
                    Track(track.id, title, Artist(track.artist?.id, nickname))
                )
                MetadataService.loadAllTracks(context, true)
                saved = true
            }

            if (saved) {
                parentFragmentManager.popBackStack()
            } else {
                saveButton.isEnabled = true
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            val originalTitle = track.title
            val currentTitle = titleInput.text.toString()
            val originalArtistNickname = track.artist?.nickname
            val currentArtistNickname = artistInput.text.toString()

            saveButton.isEnabled = !Objects.equals(currentTitle, originalTitle) ||
                    !Objects.equals(currentArtistNickname, originalArtistNickname)
        }

    }
}
