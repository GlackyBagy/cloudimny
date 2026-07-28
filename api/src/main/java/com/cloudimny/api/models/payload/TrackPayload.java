package com.cloudimny.api.models.payload;

import java.util.UUID;

public record TrackPayload(UUID id, String title, ArtistPayload artistPayload) {
}
