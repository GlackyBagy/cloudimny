package com.cloudimny.views.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.models.meta.Track
import java.util.UUID

class SelectableTrackAdapter(
    private val tracks: List<Track>
) : RecyclerView.Adapter<SelectableTrackAdapter.TrackViewHolder>() {
    val selectedTrackIds = mutableSetOf<UUID>()

    inner class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.track_title)
        val artist: TextView = view.findViewById(R.id.track_artist)
        val checkbox: CheckBox = view.findViewById(R.id.track_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_checkable, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.title.text = track.title
        holder.artist.text = track.artist?.nickname

        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = track.id in selectedTrackIds
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            val trackId = track.id ?: return@setOnCheckedChangeListener
            if (isChecked) selectedTrackIds += trackId else selectedTrackIds -= trackId
        }

        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
    }

    override fun getItemCount(): Int = tracks.size
}
