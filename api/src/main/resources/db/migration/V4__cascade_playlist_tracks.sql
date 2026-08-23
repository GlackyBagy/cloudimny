ALTER TABLE playlist_tracks
    DROP CONSTRAINT playlist_tracks_track_id_fkey,
    ADD CONSTRAINT playlist_tracks_track_id_fkey
        FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE;

ALTER TABLE playlist_tracks
    DROP CONSTRAINT playlist_tracks_playlist_id_fkey,
    ADD CONSTRAINT playlist_tracks_playlist_id_fkey
        FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE;
