package com.openchord.server.playlist;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.openchord.server.support.AuthenticatedIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class PlaylistGraphQlIntegrationTests extends AuthenticatedIntegrationTest {
    @Test
    void lifecyclePreservesTrackOrderAndIdempotency() throws Exception {
        MvcResult created =
                mvc.perform(
                                post("/graphql")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                graphql(
                                                        """
                                                        mutation { createPlaylist(name: "Night drive") { id name tracks { id } } }
                                                        """)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.createPlaylist.name", is("Night drive")))
                        .andExpect(jsonPath("$.data.createPlaylist.tracks.length()", is(0)))
                        .andReturn();
        String playlistId =
                JsonPath.read(created.getResponse().getContentAsString(), "$.data.createPlaylist.id");

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(addTrackMutation(playlistId)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.data.addTrackToPlaylist.tracks[0].id",
                                is(track.getId().toString())));

        // A retried client mutation must not create duplicate playlist rows.
        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(addTrackMutation(playlistId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addTrackToPlaylist.tracks.length()", is(1)));

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(graphql("query { playlists { id name tracks { id } } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.playlists[0].id", is(playlistId)))
                .andExpect(
                        jsonPath(
                                "$.data.playlists[0].tracks[0].id",
                                is(track.getId().toString())));

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        graphql(
                                                "mutation { deletePlaylist(id: \"%s\") }"
                                                        .formatted(playlistId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletePlaylist", is(true)));
    }

    private String addTrackMutation(String playlistId) throws Exception {
        return graphql(
                """
                mutation { addTrackToPlaylist(playlistId: "%s", trackId: "%s") { tracks { id title } } }
                """
                        .formatted(playlistId, track.getId()));
    }
}
