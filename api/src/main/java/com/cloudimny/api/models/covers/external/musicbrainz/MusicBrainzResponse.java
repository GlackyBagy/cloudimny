package com.cloudimny.api.models.covers.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MusicBrainzResponse(List<RecordingResponse> recordings) {
}
