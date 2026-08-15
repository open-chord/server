package com.openchord.server.admin;

import com.openchord.server.admin.importing.AlbumImportController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owner-only administration API for catalog inspection and direct track mutations.
 *
 * <p>Album-sized, reviewable uploads use {@link AlbumImportController}; this controller serves the
 * simpler single-track workflow.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminCatalogService catalog;

    public AdminController(AdminCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/catalog")
    public List<AdminAlbumView> catalog() {
        return catalog.catalog();
    }

    @PostMapping(path = "/tracks", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTrackView createTrack(
            @RequestParam String artist,
            @RequestParam String album,
            @RequestParam int releaseYear,
            @RequestParam String title,
            @RequestParam(defaultValue = "1") int discNumber,
            @RequestParam int trackNumber,
            @RequestParam long durationMs,
            @RequestParam(defaultValue = "") String lyrics,
            @RequestParam MultipartFile audio,
            @RequestParam(required = false) MultipartFile artwork)
            throws IOException {
        return catalog.createTrack(
                artist,
                album,
                releaseYear,
                title,
                discNumber,
                trackNumber,
                durationMs,
                lyrics,
                audio,
                artwork);
    }

    @PutMapping("/tracks/{id}/lyrics")
    public AdminTrackView replaceLyrics(
            @PathVariable UUID id, @RequestBody LyricsRequest request) {
        return catalog.replaceLyrics(id, request.lyrics());
    }

    @GetMapping("/tracks/{id}/lyrics")
    public AdminLyricsDocumentView lyrics(@PathVariable UUID id) {
        return catalog.lyrics(id);
    }

    @PutMapping("/tracks/{id}/lyrics/source")
    public AdminLyricsDocumentView replaceLyricsSource(
            @PathVariable UUID id, @RequestBody LyricsSourceRequest request) {
        return catalog.replaceLyricsSource(id, request.sourceText());
    }

    @PostMapping("/tracks/{id}/lyrics/alignment")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AdminLyricsDocumentView alignLyrics(@PathVariable UUID id) {
        return catalog.alignLyrics(id);
    }

    /** Synchronized LRC or plain text submitted by an owner. */
    public record LyricsRequest(String lyrics) {
    }

    /** Authoritative unsynchronized source submitted before alignment. */
    public record LyricsSourceRequest(String sourceText) {
    }
}
