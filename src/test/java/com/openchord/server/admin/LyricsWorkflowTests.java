package com.openchord.server.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openchord.server.catalog.LyricLine;
import com.openchord.server.catalog.LyricsStatus;
import com.openchord.server.catalog.Track;

import java.util.List;

import org.junit.jupiter.api.Test;

class LyricsWorkflowTests {
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
    void synchronizedImportKeepsExistingLrcCompatibility() {
        Track track = new Track("Song", 60_000, 1, 1, "tracks/song.m4a", "audio/mp4");
        String source = "[00:01.25] First line\n[00:04.500] Second line";

        track.replaceSynchronizedLyrics(source, AdminCatalogService.parseLyrics(source, 60_000));

        assertEquals(LyricsStatus.SYNCED, track.getLyricsStatus());
        assertEquals(2, track.getLyrics().size());
        assertEquals(1_250, track.getLyrics().get(0).getStartMs());
        assertEquals(4_500, track.getLyrics().get(0).getEndMs());
        assertEquals(60_000, track.getLyrics().get(1).getEndMs());
    }
}
