CREATE TABLE releases
(
    id             UUID PRIMARY KEY,
    type           VARCHAR(15) NOT NULL DEFAULT 'Album',
    release_date   DATE,
    cover_resolved BOOLEAN,
    timestamp      TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

ALTER TABLE tracks
    ADD COLUMN release_id UUID REFERENCES releases (id);