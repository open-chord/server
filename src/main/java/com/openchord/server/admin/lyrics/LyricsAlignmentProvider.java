package com.openchord.server.admin.lyrics;

import java.nio.file.Path;
import java.util.List;

/** Adapter for a replaceable local speech and forced-alignment implementation. */
public interface LyricsAlignmentProvider {
    boolean isAvailable();

    AlignmentResult align(Path audio, String sourceText, long durationMs) throws Exception;

    record AlignmentResult(String engine, float averageConfidence, List<AlignedLine> lines) {
    }

    record AlignedLine(String text, long startMs, long endMs, float confidence) {
    }
}
