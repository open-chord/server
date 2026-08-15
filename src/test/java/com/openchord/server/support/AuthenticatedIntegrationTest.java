package com.openchord.server.support;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openchord.server.catalog.Album;
import com.openchord.server.catalog.AlbumRepository;
import com.openchord.server.catalog.Artist;
import com.openchord.server.catalog.ArtistRepository;
import com.openchord.server.catalog.LyricLine;
import com.openchord.server.catalog.Track;
import com.openchord.server.playlist.PlaylistRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shared authenticated fixture for HTTP integration tests.
 *
 * <p>Each test starts from the same minimal catalog while PostgreSQL and the Spring context are
 * reused across test classes. Product-specific assertions stay in their owning packages.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig.class)
public abstract class AuthenticatedIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static String ownerToken;

    @Autowired protected MockMvc mvc;
    @Autowired protected WebApplicationContext context;
    @Autowired protected ArtistRepository artists;
    @Autowired protected AlbumRepository albums;
    @Autowired protected PlaylistRepository playlists;

    protected Album album;
    protected Track track;

    @BeforeEach
    final void resetFixture() throws Exception {
        if (ownerToken == null) {
            MvcResult setup =
                    mvc.perform(
                                    post("/api/auth/setup")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    """
                                                    {"username":"owner","displayName":"Owner","password":"correct-horse-battery","deviceName":"Tests","mode":"FAMILY"}
                                                    """))
                            .andExpect(status().isOk())
                            .andReturn();
            ownerToken = JsonPath.read(setup.getResponse().getContentAsString(), "$.accessToken");
        }

        mvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .apply(springSecurity())
                        .defaultRequest(get("/").header("Authorization", "Bearer " + ownerToken))
                        .build();

        playlists.deleteAll();
        albums.deleteAll();
        artists.deleteAll();

        Artist artist = artists.save(new Artist("Aurora Lines"));
        album = new Album("Afterglow", 2026, null, artist);
        track = new Track("Night Drive", 96_000, 1, 1, "tracks/night-drive.m4a", "audio/mp4");
        track.addLyricLine(new LyricLine("Streetlights drawing silver lines", 0, 8_000));
        album.addTrack(track);
        album = albums.saveAndFlush(album);

        Path audio = mediaRoot().resolve("tracks/night-drive.m4a");
        Files.createDirectories(audio.getParent());
        Files.write(audio, "0123456789".getBytes());
    }

    protected static Path mediaRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), "openchord-test-media");
    }

    protected static String graphql(String query) throws JsonProcessingException {
        return json(Map.of("query", query));
    }

    protected static String json(Object value) throws JsonProcessingException {
        return JSON.writeValueAsString(value);
    }
}
