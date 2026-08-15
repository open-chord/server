ALTER TABLE tracks
    ADD COLUMN lyrics_alignment_engine VARCHAR(255),
    ADD COLUMN lyrics_alignment_error TEXT,
    ADD COLUMN lyrics_average_confidence REAL;

ALTER TABLE lyric_lines
    ADD COLUMN confidence REAL;

ALTER TABLE tracks DROP CONSTRAINT ck_tracks_lyrics_status;
ALTER TABLE tracks
    ADD CONSTRAINT ck_tracks_lyrics_status
        CHECK (lyrics_status IN ('EMPTY', 'UNSYNCED', 'PROCESSING', 'NEEDS_REVIEW', 'SYNCED', 'FAILED'));

ALTER TABLE tracks
    ADD CONSTRAINT ck_tracks_lyrics_average_confidence
        CHECK (lyrics_average_confidence IS NULL OR (lyrics_average_confidence >= 0 AND lyrics_average_confidence <= 1));

ALTER TABLE lyric_lines
    ADD CONSTRAINT ck_lyric_lines_confidence
        CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1));
