package com.openchord.server.admin.archive;

/** Result of a committed portable archive import. */
public record ArchiveImportSummary(
        int albums, int tracks, int playlists, int skippedAlbums) {
}
