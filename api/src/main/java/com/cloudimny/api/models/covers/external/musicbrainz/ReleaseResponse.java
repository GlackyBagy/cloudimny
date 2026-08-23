package com.cloudimny.api.models.covers.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseResponse(UUID id,
                              String title,
                              String status,
                              @JsonProperty("release-group") ReleaseGroupResponse releaseGroup,
                              String date) {
}
