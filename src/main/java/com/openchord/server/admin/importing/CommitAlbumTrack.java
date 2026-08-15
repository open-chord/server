package com.openchord.server.admin.importing;

/** Owner-reviewed metadata for one staged audio file. */
public record CommitAlbumTrack(
        String stagedFile,
        String title,
        int discNumber,
        int number,
        long durationMs,
        String sourceFormat,
        String originalFilename) {
}
