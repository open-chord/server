package com.openchord.server.admin.lyrics;

import com.openchord.server.catalog.LyricLine;
import com.openchord.server.catalog.Track;
import com.openchord.server.catalog.TrackRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies asynchronous alignment outcomes in short database transactions. */
@Service
class LyricsAlignmentPersistence {
    private final TrackRepository tracks;

    LyricsAlignmentPersistence(TrackRepository tracks) {
        this.tracks = tracks;
    }

    @Transactional
    void complete(UUID trackId, LyricsAlignmentProvider.AlignmentResult result) {
        Track track = requiredTrack(trackId);
        validate(result, track.getDurationMs());
        List<LyricLine> lines =
                result.lines().stream()
                        .map(line ->
                                new LyricLine(
                                        line.text().strip(),
                                        line.startMs(),
                                        line.endMs(),
                                        line.confidence()))
                        .toList();
        track.replaceLyrics(List.of());
        tracks.saveAndFlush(track);
        track.completeLyricsAlignment(lines, result.engine(), result.averageConfidence());
        tracks.saveAndFlush(track);
    }

    @Transactional
    void fail(UUID trackId, String error) {
        Track track = requiredTrack(trackId);
        track.failLyricsAlignment(error);
        tracks.saveAndFlush(track);
    }

    private Track requiredTrack(UUID id) {
        return tracks
                .findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Track not found"));
    }

    private static void validate(LyricsAlignmentProvider.AlignmentResult result, long durationMs) {
        if (result.engine() == null || result.engine().isBlank()) {
            throw new IllegalArgumentException("Alignment engine name is required");
        }
        if (result.averageConfidence() < 0 || result.averageConfidence() > 1) {
            throw new IllegalArgumentException("Average confidence must be between 0 and 1");
        }
        long previousStart = -1;
        for (LyricsAlignmentProvider.AlignedLine line : result.lines()) {
            if (line.text() == null || line.text().isBlank()) {
                throw new IllegalArgumentException("Aligned lyric text is required");
            }
            if (line.startMs() <= previousStart || line.startMs() < 0 || line.endMs() <= line.startMs()
                    || line.endMs() > durationMs) {
                throw new IllegalArgumentException("Aligned lyric intervals are invalid");
            }
            if (line.confidence() < 0 || line.confidence() > 1) {
                throw new IllegalArgumentException("Line confidence must be between 0 and 1");
            }
            previousStart = line.startMs();
        }
    }
}
