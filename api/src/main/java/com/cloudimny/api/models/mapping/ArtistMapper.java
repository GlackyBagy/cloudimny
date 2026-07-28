package com.cloudimny.api.models.mapping;

import com.cloudimny.api.models.dto.ArtistDTO;
import com.cloudimny.api.models.entities.Artist;
import com.cloudimny.api.models.payload.ArtistPayload;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ArtistMapper {
    @Mapping(target = "id", expression = "java(id)")
    Artist toArtist(ArtistPayload payload, @Context UUID id);

    ArtistDTO toDTO(Artist artist);
}
