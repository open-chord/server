package com.openchord.server.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrackLyricsTests {
    @Test
    void sourceTextInvalidatesPreviouslySynchronizedLines() {
        Track track = new Track("Song", 60_000, 1, 1, "tracks/song.m4a", "audio/mp4");
        track.replaceSynchronizedLyrics(
                "[00:01.00] Old line", List.of(new LyricLine("Old line", 1_000, 60_000)));

        track.replaceLyricsSource("New first line\r\nNew second line");

        assertEquals("New first line\nNew second line", track.getLyricsSource());
        assertEquals(LyricsStatus.UNSYNCED, track.getLyricsStatus());
        assertTrue(track.getLyrics().isEmpty());
    }

    @Test
    void alignmentResultRequiresReviewAndRetainsConfidence() {
        Track track = new Track("Song", 60_000, 1, 1, "tracks/song.m4a", "audio/mp4");
        track.replaceLyricsSource("First line\nSecond line");

        track.beginLyricsAlignment();
        assertEquals(LyricsStatus.PROCESSING, track.getLyricsStatus());

        track.completeLyricsAlignment(
                List.of(
                        new LyricLine("First line", 1_000, 4_500, 0.96f),
                        new LyricLine("Second line", 4_500, 9_000, 0.88f)),
                "whisper-test",
                0.92f);

        assertEquals(LyricsStatus.NEEDS_REVIEW, track.getLyricsStatus());
        assertEquals("whisper-test", track.getLyricsAlignmentEngine());
        assertEquals(0.92f, track.getLyricsAverageConfidence());
        assertEquals(0.96f, track.getLyrics().getFirst().getConfidence());
    }
}
