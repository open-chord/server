package com.openchord.server.admin.archive;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openchord.server.catalog.Album;
import com.openchord.server.playlist.Playlist;
import com.openchord.server.support.AuthenticatedIntegrationTest;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

class OpenChordArchiveIntegrationTests extends AuthenticatedIntegrationTest {
    @Test
    void exportContainsManifestCatalogAndDeduplicatedMedia() throws Exception {
        Map<String, byte[]> entries = unzip(exportArchive());

        assertNotNull(entries.get("manifest.json"));
        assertNotNull(entries.get("catalog/artists.jsonl"));
        assertNotNull(entries.get("catalog/albums.jsonl"));
        assertNotNull(entries.get("catalog/tracks.jsonl"));
        assertNotNull(entries.get("catalog/assets.jsonl"));
        assertTrue(new String(entries.get("manifest.json")).contains("\"format\":\"openchord\""));
        assertTrue(new String(entries.get("catalog/tracks.jsonl")).contains("\"purpose\":\"playable\""));
        assertTrue(new String(entries.get("catalog/tracks.jsonl")).contains("\"purpose\":\"original\""));
        assertEquals(
                1,
                entries.keySet().stream()
                        .filter(path -> path.startsWith("assets/sha256/"))
                        .count());
    }

    @Test
    void exportedArchiveRestoresCatalogPlaylistAndMedia() throws Exception {
        Path playlistArtwork = mediaRoot().resolve("playlist-artwork/night-drive.jpg");
        Files.createDirectories(playlistArtwork.getParent());
        byte[] artworkBytes = "playlist-artwork".getBytes();
        Files.write(playlistArtwork, artworkBytes);
        Playlist playlist =
                new Playlist(
                        "Late night", "Two-lane glow", Instant.parse("2026-08-01T20:00:00Z"));
        playlist.addTrack(track, Instant.parse("2026-08-01T20:01:00Z"));
        playlist.setArtwork(
                "playlist-artwork/night-drive.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                Instant.parse("2026-08-01T20:02:00Z"));
        playlists.saveAndFlush(playlist);

        byte[] archive = exportArchive();
        playlists.deleteAll();
        albums.deleteAll();
        artists.deleteAll();

        MockMultipartFile upload =
                new MockMultipartFile(
                        "archive",
                        "library.openchord",
                        "application/vnd.openchord.archive+zip",
                        archive);
        mvc.perform(multipart("/api/admin/openchord/import").file(upload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.albums", is(1)))
                .andExpect(jsonPath("$.tracks", is(1)))
                .andExpect(jsonPath("$.playlists", is(1)))
                .andExpect(jsonPath("$.skippedAlbums", is(0)));

        Album restored = albums.findAllDetailed().getFirst();
        assertEquals("Afterglow", restored.getTitle());
        assertEquals("Night Drive", restored.getTracks().getFirst().getTitle());
        assertTrue(Files.exists(mediaRoot().resolve(restored.getTracks().getFirst().getAudioPath())));

        Playlist restoredPlaylist = playlists.findAllDetailed().getFirst();
        assertEquals("Late night", restoredPlaylist.getName());
        assertEquals("Two-lane glow", restoredPlaylist.getDescription());
        assertEquals(track.getTitle(), restoredPlaylist.getEntries().getFirst().getTrack().getTitle());
        assertEquals(MediaType.IMAGE_JPEG_VALUE, restoredPlaylist.getArtworkContentType());
        assertArrayEquals(
                artworkBytes,
                Files.readAllBytes(mediaRoot().resolve(restoredPlaylist.getArtworkPath())));
    }

    private byte[] exportArchive() throws Exception {
        MvcResult pending =
                mvc.perform(get("/api/admin/openchord/export"))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        return mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openchord.archive+zip"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private static Map<String, byte[]> unzip(byte[] archive) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), input.readAllBytes());
            }
        }
        return entries;
    }
}
