package com.cloudimny.api.models.covers.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordingResponse(UUID id,
                                String title,
                                List<ReleaseResponse> releases) {
}
