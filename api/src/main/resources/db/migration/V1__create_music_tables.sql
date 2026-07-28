CREATE TABLE artists
(
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nickname VARCHAR(255) NOT NULL
);

CREATE TABLE tracks
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    artist_id   UUID         NOT NULL REFERENCES artists (id),
    timestamp   TIMESTAMP    NOT NULL DEFAULT NOW() AT TIME ZONE 'UTC',
    storage_key VARCHAR(255)
);

CREATE INDEX idx_tracks_artist_id ON tracks (artist_id);

CREATE TABLE playlists
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid()
);

CREATE TABLE playlist_tracks
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id UUID NOT NULL REFERENCES playlists (id),
    track_id    UUID NOT NULL REFERENCES tracks (id),
    position    INT  NOT NULL
);

CREATE INDEX idx_playlist_tracks_playlist_id ON playlist_tracks (playlist_id);
CREATE INDEX idx_playlist_tracks_track_id ON playlist_tracks (track_id);
