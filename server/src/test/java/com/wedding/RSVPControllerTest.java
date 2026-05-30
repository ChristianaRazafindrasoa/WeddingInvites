package com.wedding;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
    scripts = "/sql/set-good-known-state.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class RSVPControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitRsvp_returnsOk_whenMainGuestAndPlusOneValid() throws Exception {
        String requestBody = "{" +
                "\"mainGuestName\":\"Foo Test\"," +
                "\"plusOneName\":\"Bar Test\"," +
                "\"isAccepted\":true" +
                "}";

        mockMvc.perform(post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainGuestName")
                        .value("Foo Test"))
                .andExpect(jsonPath("$.plusOneName")
                        .value("Bar Test"))
                .andExpect(jsonPath("$.message")
                        .value("Thank you for attending."));
    }

    @Test
    void submitRsvp_returnsBadRequest_whenAlreadySubmitted() throws Exception {
        String requestBody = "{" +
                "\"mainGuestName\":\"Foo Test\"," +
                "\"plusOneName\":\"Bar Test\"," +
                "\"isAccepted\":true" +
                "}";

        mockMvc.perform(post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rsvp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("RSVP already submitted."));
    }
}
