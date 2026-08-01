package com.cloudimny.api.models.payload;

import java.util.List;
import java.util.UUID;

public record PlaylistPayload(String name, List<UUID> trackIds) {
}
