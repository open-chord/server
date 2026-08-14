package com.openchord.server.playlist;

import com.openchord.server.catalog.Track;
import com.openchord.server.catalog.TrackRepository;
import com.openchord.server.config.OpenChordProperties;
import com.openchord.server.auth.OpenChordUser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Transaction boundary for playlist lifecycle and ordered membership mutations. */
@Service
public class PlaylistService {
    private final PlaylistRepository playlists;
    private final TrackRepository tracks;
    private final Path mediaRoot;
    private final Clock clock = Clock.systemUTC();

    public PlaylistService(
            PlaylistRepository playlists, TrackRepository tracks, OpenChordProperties properties) {
        this.playlists = playlists;
        this.tracks = tracks;
        this.mediaRoot = properties.mediaRoot().toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<Playlist> playlists(OpenChordUser owner) {
        return playlists.findAllDetailedByOwnerId(owner.getId());
    }

    @Transactional(readOnly = true)
    public Playlist playlist(OpenChordUser owner, UUID id) {
        return findDetailed(owner, id);
    }

    @Transactional
    public Playlist create(OpenChordUser owner, String name) {
        return create(owner, name, "", null);
    }

    @Transactional
    public Playlist create(OpenChordUser owner, String name, String description, MultipartFile artwork) {
        Instant now = clock.instant();
        Playlist playlist =
                playlists.saveAndFlush(
                        new Playlist(normalizeName(name), normalizeDescription(description), now, owner));
        if (artwork == null || artwork.isEmpty()) {
            return playlist;
        }
        String contentType = artwork.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Playlist artwork must be an image");
        }
        String extension =
                switch (contentType) {
                    case "image/png" -> ".png";
                    case "image/webp" -> ".webp";
                    default -> ".jpg";
                };
        String relativePath = "playlist-artwork/" + playlist.getId() + extension;
        Path target = mediaRoot.resolve(relativePath).normalize();
        if (!target.startsWith(mediaRoot)) {
            throw new IllegalArgumentException("Invalid artwork path");
        }
        try {
            Files.createDirectories(target.getParent());
            artwork.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store playlist artwork", exception);
        }
        playlist.setArtwork(relativePath, contentType, now);
        return playlists.saveAndFlush(playlist);
    }

    @Transactional
    public Playlist rename(OpenChordUser owner, UUID id, String name) {
        Playlist playlist = findDetailed(owner, id);
        playlist.rename(normalizeName(name), clock.instant());
        return playlists.saveAndFlush(playlist);
    }

    @Transactional
    public boolean delete(OpenChordUser owner, UUID id) {
        if (playlists.findDetailedByIdAndOwnerId(id, owner.getId()).isEmpty()) {
            return false;
        }
        playlists.deleteById(id);
        return true;
    }

    @Transactional
    public Playlist addTrack(OpenChordUser owner, UUID playlistId, UUID trackId) {
        Playlist playlist = findDetailed(owner, playlistId);
        Track track =
                tracks.findDetailedById(trackId)
                        .orElseThrow(() -> new PlaylistNotFoundException("Track not found"));
        playlist.addTrack(track, clock.instant());
        playlists.saveAndFlush(playlist);
        return findDetailed(owner, playlistId);
    }

    @Transactional
    public Playlist removeTrack(OpenChordUser owner, UUID playlistId, UUID trackId) {
        Playlist playlist = findDetailed(owner, playlistId);
        if (!playlist.removeTrack(trackId, clock.instant())) {
            throw new PlaylistNotFoundException("Track is not in this playlist");
        }
        playlists.saveAndFlush(playlist);
        return findDetailed(owner, playlistId);
    }

    @Transactional
    public Playlist moveTrack(OpenChordUser owner, UUID playlistId, UUID trackId, int position) {
        Playlist playlist = findDetailed(owner, playlistId);
        playlist.moveTrack(trackId, position, clock.instant());
        playlists.saveAndFlush(playlist);
        return findDetailed(owner, playlistId);
    }

    private Playlist findDetailed(OpenChordUser owner, UUID id) {
        return playlists.findDetailedByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found"));
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new IllegalArgumentException("Playlist name must contain 1 to 120 characters");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        String normalized = description == null ? "" : description.strip();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Playlist description must not exceed 500 characters");
        }
        return normalized;
    }
}
