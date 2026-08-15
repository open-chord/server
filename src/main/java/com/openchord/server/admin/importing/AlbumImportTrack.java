package com.openchord.server.admin.importing;

import java.util.List;

/** Detected metadata and conversion plan for one staged audio file. */
public record AlbumImportTrack(
        String stagedFile,
        String originalFilename,
        String title,
        int discNumber,
        int number,
        long durationMs,
        String sourceFormat,
        boolean willTranscode,
        List<String> issues) {
}
