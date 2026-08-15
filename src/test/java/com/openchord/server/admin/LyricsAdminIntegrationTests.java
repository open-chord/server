package com.openchord.server.admin;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openchord.server.support.AuthenticatedIntegrationTest;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LyricsAdminIntegrationTests extends AuthenticatedIntegrationTest {
    @Test
    void replacementCanReuseTimestampsAndUpdatesClientProjection() throws Exception {
        mvc.perform(
                        put("/api/admin/tracks/{id}/lyrics", track.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json(
                                                Map.of(
                                                        "lyrics",
                                                        "[00:00.000]Replacement line\n[00:09.000]Second line"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lyricLines", is(2)));

        mvc.perform(
                        post("/graphql")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        graphql(
                                                "query { albums { tracks { id lyrics { text startMs } } } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.albums[0].tracks[0].lyrics.length()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.data.albums[0].tracks[0].lyrics[0].text",
                                is("Replacement line")))
                .andExpect(jsonPath("$.data.albums[0].tracks[0].lyrics[0].startMs", is(0)));
    }
}
