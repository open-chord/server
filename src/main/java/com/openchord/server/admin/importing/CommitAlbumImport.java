package com.openchord.server.admin.importing;

import java.util.List;

/** Owner-reviewed album metadata submitted for import commit. */
public record CommitAlbumImport(
        String artist,
        String album,
        int year,
        String artworkFile,
        List<CommitAlbumTrack> tracks) {
}
