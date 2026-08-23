package com.cloudimny.views.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cloudimny.R
import com.cloudimny.covers.CoverLoader
import com.cloudimny.models.meta.Playlist

class PlaylistAdapter(
    private val playlists: List<Playlist>,
    private val onPlaylistClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.playlist_cover)
        val name: TextView = view.findViewById(R.id.playlist_name)
        val trackCount: TextView = view.findViewById(R.id.playlist_track_count)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistClick(playlists[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.name.text = playlist.name
        holder.trackCount.text = holder.itemView.resources.getString(
            R.string.playlist_track_count,
            playlist.songList.size
        )
        // плейлист сам по себе обложки не имеет, поэтому берётся обложка первого трека
        CoverLoader.load(holder.cover, playlist.songList.firstOrNull()?.id)
    }

    override fun getItemCount(): Int = playlists.size
}
