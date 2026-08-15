package com.openchord.server.graphql;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openchord.server.support.AuthenticatedIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CatalogGraphQlIntegrationTests extends AuthenticatedIntegrationTest {
    @Test
    void catalogPlaybackAndRecentFlow() throws Exception {
        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphql("{ albums { id } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.albums[0].id", is(album.getId().toString())));

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        graphql(
                                                """
                                                query { albums(search: "Aurora") { id title year artist { name } tracks { id durationMs streamUrl lyrics { text endMs } } } }
                                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.albums[0].id", is(album.getId().toString())))
                .andExpect(jsonPath("$.data.albums[0].tracks.length()", is(1)))
                .andExpect(jsonPath("$.data.albums[0].artist.name", is("Aurora Lines")))
                .andExpect(jsonPath("$.data.albums[0].tracks[0].lyrics[0].endMs", is(8000)));

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        graphql(
                                                """
                                                mutation { recordPlayback(input: { trackId: "%s", positionMs: 120000, completed: true }) { trackId positionMs completed } }
                                                """
                                                        .formatted(track.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordPlayback.trackId", is(track.getId().toString())))
                .andExpect(jsonPath("$.data.recordPlayback.positionMs", is(96000)))
                .andExpect(jsonPath("$.data.recordPlayback.completed", is(true)));

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphql("{ recentlyPlayed { id } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentlyPlayed[0].id", is(album.getId().toString())));
    }
}
