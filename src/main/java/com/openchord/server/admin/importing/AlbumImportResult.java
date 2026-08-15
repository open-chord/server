package com.openchord.server.admin.importing;

import java.util.UUID;

/** Summary returned after a reviewed album import has been committed. */
public record AlbumImportResult(
        UUID albumId, String album, int importedTracks, int transcodedTracks) {
}
