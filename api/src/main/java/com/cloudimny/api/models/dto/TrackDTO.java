package com.cloudimny.api.models.dto;

import java.time.Instant;
import java.util.UUID;

public record TrackDTO(UUID id, String title, ArtistDTO artist, Instant uploadedAt) {
}
