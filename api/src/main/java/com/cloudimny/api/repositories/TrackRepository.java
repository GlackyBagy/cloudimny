package com.cloudimny.api.repositories;

import com.cloudimny.api.models.entities.Track;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TrackRepository extends R2dbcRepository<Track, UUID> {

}
