package com.cloudimny.views.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.covers.CoverLoader
import com.cloudimny.models.meta.Track

class TrackAdapter(
    private val tracks: List<Track>,
    private val onTrackClick: (Track) -> Unit,
    private val onTrackLongClick: (Track, View) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {
    inner class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.track_cover)
        val title: TextView = view.findViewById(R.id.track_title)

        val artist: TextView = view.findViewById(R.id.track_artist)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION)
                    onTrackClick(tracks[position])
            }

            view.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTrackLongClick(tracks[position], view)
                }
                true // event handled
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
        CoverLoader.load(holder.cover, track.id)
    }

    override fun getItemCount(): Int = tracks.size
}
