ALTER TABLE tracks
    ADD COLUMN lyrics_source TEXT,
    ADD COLUMN lyrics_status VARCHAR(32) NOT NULL DEFAULT 'EMPTY';

UPDATE tracks
SET lyrics_status = 'SYNCED'
WHERE EXISTS (
    SELECT 1
    FROM lyric_lines
    WHERE lyric_lines.track_id = tracks.id
);

ALTER TABLE tracks
    ADD CONSTRAINT ck_tracks_lyrics_status
        CHECK (lyrics_status IN ('EMPTY', 'UNSYNCED', 'SYNCED'));
