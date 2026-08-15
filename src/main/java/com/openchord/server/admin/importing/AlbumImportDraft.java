package com.openchord.server.admin.importing;

import java.util.List;
import java.util.UUID;

/** Metadata detected during analysis and presented for owner review. */
public record AlbumImportDraft(
        UUID id,
        String artist,
        String album,
        int year,
        String artworkFile,
        List<AlbumImportTrack> tracks,
        List<String> issues) {
}
