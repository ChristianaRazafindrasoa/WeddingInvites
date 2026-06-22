package com.wedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:sql/set-known-good-state.sql")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${admin.password}")
    private String adminPassword;

    private String getToken() throws Exception {
        String body = """
                {"password": "%s"}
                """.formatted(adminPassword);

        String response = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new ObjectMapper().readTree(response).get("token").asText();
    }

    @Test
    void getAllGuestsReturnsGuestList() throws Exception {
        String token = getToken();

        mockMvc.perform(get("/api/guests")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Foo Test"))
                .andExpect(jsonPath("$[1].fullName").value("Bar Test"))
                .andExpect(jsonPath("$[2].fullName").value("Test McTest"));
    }

    @Test
    void getAllRsvpsReturnsRsvpList() throws Exception {
        String token = getToken();

        mockMvc.perform(get("/api/rsvps")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].token").value("abc123"))
                .andExpect(jsonPath("$[0].mainGuest.fullName").value("Foo Test"))
                .andExpect(jsonPath("$[0].plusOne.fullName").value("Bar Test"))
                .andExpect(jsonPath("$[1].token").value("123abc"))
                .andExpect(jsonPath("$[1].mainGuest.fullName").value("Test McTest"));
    }

    @Test
    void getAllGuestsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/guests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllRsvpsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/rsvps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }
}
