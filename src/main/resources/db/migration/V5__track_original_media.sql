ALTER TABLE tracks
    ADD COLUMN original_path VARCHAR(1024),
    ADD COLUMN original_content_type VARCHAR(127),
    ADD COLUMN original_filename VARCHAR(1024);

UPDATE tracks
SET original_path = audio_path,
    original_content_type = content_type,
    original_filename = regexp_replace(audio_path, '^.*/', '');

ALTER TABLE tracks
    ADD CONSTRAINT uq_tracks_original_path UNIQUE (original_path);
