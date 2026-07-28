package com.cloudimny.api.models.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("playlists")
public record Playlist(@Id UUID id) {
}
