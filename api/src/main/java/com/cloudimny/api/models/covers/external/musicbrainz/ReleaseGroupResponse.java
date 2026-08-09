package com.cloudimny.api.models.covers.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseGroupResponse(UUID id,
                                   @JsonProperty("primary-type") String primaryType) {
}
