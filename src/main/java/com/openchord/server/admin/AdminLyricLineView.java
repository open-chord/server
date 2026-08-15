package com.openchord.server.admin;

import com.openchord.server.catalog.LyricLine;

import java.util.UUID;

/** One synchronized line in an editable administration lyrics document. */
public record AdminLyricLineView(
        UUID id, String text, long startMs, long endMs, Float confidence) {
    static AdminLyricLineView from(LyricLine line) {
        return new AdminLyricLineView(
                line.getId(),
                line.getText(),
                line.getStartMs(),
                line.getEndMs(),
                line.getConfidence());
    }
}
