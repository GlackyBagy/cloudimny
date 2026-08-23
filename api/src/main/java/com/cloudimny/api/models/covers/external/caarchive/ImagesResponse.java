package com.cloudimny.api.models.covers.external.caarchive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImagesResponse(List<Image> images, String release) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(Long id, URI image, Map<String, URI> thumbnails,
                        boolean front, boolean back, boolean approved) {
    }

}
