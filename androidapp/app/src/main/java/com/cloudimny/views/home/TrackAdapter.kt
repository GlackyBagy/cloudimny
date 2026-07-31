package com.cloudimny.views.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.models.meta.Track

class TrackAdapter(
    private val tracks: List<Track>,
    private val onTrackClick: (Track) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    inner class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.track_title)
        val artist: TextView = view.findViewById(R.id.track_artist)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTrackClick(tracks[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        holder.title.text = track.title
        holder.artist.text = track.artist?.nickname
    }

    override fun getItemCount(): Int = tracks.size
}
