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
@Sql(scripts = "classpath:sql/set-known-good-state.sql")
class RSVPControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void submitRsvpReturnsOkWhenMainGuestAndPlusOneValid() throws Exception {
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
    void submitRsvpRturnsBadRequestWhenAlreadySubmitted() throws Exception {
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
