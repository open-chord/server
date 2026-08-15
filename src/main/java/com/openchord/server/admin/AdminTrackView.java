package com.openchord.server.admin;

import com.openchord.server.catalog.Track;

import java.util.UUID;

/** Compact track projection used by the administration catalog. */
public record AdminTrackView(
        UUID id, String title, long durationMs, int discNumber, int number, int lyricLines) {
    static AdminTrackView from(Track track) {
        return new AdminTrackView(
                track.getId(),
                track.getTitle(),
                track.getDurationMs(),
                track.getDiscNumber(),
                track.getNumber(),
                track.getLyrics().size());
    }
}
