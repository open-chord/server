package com.openchord.server;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openchord.server.support.AuthenticatedIntegrationTest;

import org.junit.jupiter.api.Test;

class HealthIntegrationTests extends AuthenticatedIntegrationTest {
    @Test
    void readinessReportsUp() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
