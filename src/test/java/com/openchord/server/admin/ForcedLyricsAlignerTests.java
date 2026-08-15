package com.openchord.server.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ForcedLyricsAlignerTests {
    @Test
    void alignsAuthoritativeLinesDespiteRecognitionNoise() {
        List<ForcedLyricsAligner.RecognizedWord> words =
                List.of(
                        word("I", 1_000, 1_200, 0.98f),
                        word("found", 1_210, 1_500, 0.95f),
                        word("the", 1_510, 1_650, 0.60f),
                        word("love", 1_660, 2_000, 0.94f),
                        word("for", 2_010, 2_200, 0.96f),
                        word("me", 2_210, 2_400, 0.97f),
                        word("darlin", 3_400, 3_800, 0.82f),
                        word("just", 3_810, 4_000, 0.96f),
                        word("dive", 4_010, 4_300, 0.95f),
                        word("right", 4_310, 4_600, 0.94f),
                        word("in", 4_610, 4_800, 0.98f));

        LyricsAlignmentProvider.AlignmentResult result =
                new ForcedLyricsAligner()
                        .align(
                                "I found a love for me\nDarling, just dive right in",
                                words,
                                "test-engine",
                                60_000);

        assertEquals(2, result.lines().size());
        assertEquals(1_000, result.lines().get(0).startMs());
        assertEquals(2_400, result.lines().get(0).endMs());
        assertEquals(3_400, result.lines().get(1).startMs());
        assertTrue(result.averageConfidence() > 0.7f);
    }

    private static ForcedLyricsAligner.RecognizedWord word(
            String text, long start, long end, float probability) {
        return new ForcedLyricsAligner.RecognizedWord(text, start, end, probability);
    }
}
