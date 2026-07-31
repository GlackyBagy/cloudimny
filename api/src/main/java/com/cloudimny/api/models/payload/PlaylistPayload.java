package com.cloudimny.api.models.payload;

import java.util.List;

public record PlaylistPayload(List<TrackPayload> songList) {
}
