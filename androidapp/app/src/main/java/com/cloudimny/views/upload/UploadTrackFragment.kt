package com.cloudimny.views.upload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.cloudimny.R
import com.cloudimny.server.TrackUploadWorker
import com.cloudimny.util.displayName
import com.cloudimny.views.MainActivity

class UploadTrackFragment : Fragment(R.layout.fragment_upload_track) {

    private lateinit var chosenFileName: TextView
    private lateinit var titleInput: EditText
    private lateinit var artistInput: EditText
    private lateinit var uploadButton: Button

    private var selectedFileUri: Uri? = null

    private val pickAudioFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onFileSelected(uri)
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { enqueueUpload() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setHeaderTitle(getString(R.string.upload_track_title))

        val chooseFileButton: Button = view.findViewById(R.id.choose_file_button)
        chosenFileName = view.findViewById(R.id.chosen_file_name)
        titleInput = view.findViewById(R.id.track_title_input)
        artistInput = view.findViewById(R.id.track_artist_input)
        uploadButton = view.findViewById(R.id.upload_button)

        chooseFileButton.setOnClickListener {
            pickAudioFile.launch(arrayOf("audio/*", "video/webm"))
        }

        uploadButton.setOnClickListener {
            startUpload()
        }
    }

    private fun onFileSelected(uri: Uri) {
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        selectedFileUri = uri
        chosenFileName.text = displayName(requireContext(), uri)
        uploadButton.isEnabled = true

        val (title, artist) = readMetadata(uri)
        if (!title.isNullOrBlank()) titleInput.setText(title)
        if (!artist.isNullOrBlank()) artistInput.setText(artist)
    }

    private fun readMetadata(uri: Uri): Pair<String?, String?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(requireContext(), uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            title to artist
        } catch (_: Exception) {
            null to null
        } finally {
            retriever.release()
        }
    }

    private fun startUpload() {
        if (selectedFileUri == null) return
        if (titleInput.text.isNullOrBlank() || artistInput.text.isNullOrBlank()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enqueueUpload()
        }
    }

    private fun enqueueUpload() {
        val uri = selectedFileUri ?: return
        val title = titleInput.text.toString().trim()
        val artist = artistInput.text.toString().trim()
        if (title.isEmpty() || artist.isEmpty()) return

        val inputData = Data.Builder()
            .putString(TrackUploadWorker.KEY_URI, uri.toString())
            .putString(TrackUploadWorker.KEY_TITLE, title)
            .putString(TrackUploadWorker.KEY_ARTIST, artist)
            .build()

        val request = OneTimeWorkRequestBuilder<TrackUploadWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(request)

        parentFragmentManager.popBackStack()
    }
}
