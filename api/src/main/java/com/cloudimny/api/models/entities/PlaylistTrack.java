package com.cloudimny.api.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("playlist_tracks")
public record PlaylistTrack(@Id UUID id, UUID playlistId, UUID trackId, int position) {
}
