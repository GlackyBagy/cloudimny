package com.cloudimny.api.models.mapping;

import com.cloudimny.api.models.dto.TrackDTO;
import com.cloudimny.api.models.entities.Artist;
import com.cloudimny.api.models.entities.Track;
import com.cloudimny.api.models.payload.TrackPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(uses = ArtistMapper.class,
        componentModel = "spring")
public interface TrackMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "artistId", source = "artistId")
    Track toTrack(TrackPayload payload, UUID id, UUID artistId);

    @Mapping(target = "id", source = "track.id")
    @Mapping(target = "artistDTO", source = "artist")
    @Mapping(target = "uploadedAt", source = "track.timestamp")
    TrackDTO toDTO(Track track, Artist artist);
}
