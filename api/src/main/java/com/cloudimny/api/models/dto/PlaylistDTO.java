package com.cloudimny.api.models.dto;

import java.util.List;
import java.util.UUID;

public record PlaylistDTO(UUID id, String name, List<TrackDTO> songList) {
}
