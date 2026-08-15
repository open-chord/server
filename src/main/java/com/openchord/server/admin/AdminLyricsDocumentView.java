package com.openchord.server.admin;

import com.openchord.server.catalog.LyricsStatus;
import com.openchord.server.catalog.Track;

import java.util.List;

/** Editable source, alignment state, and synchronized lines for one track. */
public record AdminLyricsDocumentView(
        String sourceText,
        LyricsStatus status,
        boolean alignmentAvailable,
        String alignmentEngine,
        String alignmentError,
        Float averageConfidence,
        List<AdminLyricLineView> lines) {
    static AdminLyricsDocumentView from(Track track, boolean alignmentAvailable) {
        return new AdminLyricsDocumentView(
                track.getLyricsSource(),
                track.getLyricsStatus(),
                alignmentAvailable,
                track.getLyricsAlignmentEngine(),
                track.getLyricsAlignmentError(),
                track.getLyricsAverageConfidence(),
                track.getLyrics().stream().map(AdminLyricLineView::from).toList());
    }
}
