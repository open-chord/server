package com.openchord.server.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.openchord.server.catalog.LyricLine;

import java.util.List;

import org.junit.jupiter.api.Test;

class LyricsParserTests {
    @Test
    void parsesSupportedLrcFractionsIntoPlaybackIntervals() {
        String source = "[00:01.25] First line\n[00:04.500] Second line";

        List<LyricLine> lyrics = AdminCatalogService.parseLyrics(source, 60_000);

        assertEquals(2, lyrics.size());
        assertEquals(1_250, lyrics.get(0).getStartMs());
        assertEquals(4_500, lyrics.get(0).getEndMs());
        assertEquals(60_000, lyrics.get(1).getEndMs());
    }
}
