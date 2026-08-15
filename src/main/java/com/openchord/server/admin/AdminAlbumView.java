package com.openchord.server.admin;

import com.openchord.server.catalog.Album;

import java.util.List;
import java.util.UUID;

/** Album projection returned by the administration catalog. */
public record AdminAlbumView(
        UUID id,
        String title,
        int year,
        String artist,
        boolean hasArtwork,
        List<AdminTrackView> tracks) {
    static AdminAlbumView from(Album album) {
        return new AdminAlbumView(
                album.getId(),
                album.getTitle(),
                album.getReleaseYear(),
                album.getArtist().getName(),
                album.getArtworkPath() != null,
                album.getTracks().stream().map(AdminTrackView::from).toList());
    }
}
