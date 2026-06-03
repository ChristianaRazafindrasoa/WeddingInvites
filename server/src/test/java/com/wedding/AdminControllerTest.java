package com.wedding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:sql/set-known-good-state.sql")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllGuestsReturnsGuestList() throws Exception {
        mockMvc.perform(get("/api/admin/guests")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Foo Test"))
                .andExpect(jsonPath("$[1].fullName").value("Bar Test"))
                .andExpect(jsonPath("$[2].fullName").value("Test McTest"));
    }

    @Test
    void getAllRsvpsReturnsRsvpList() throws Exception {
        mockMvc.perform(get("/api/admin/rsvps")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].token").value("abc123"))
                .andExpect(jsonPath("$[0].mainGuest.fullName").value("Foo Test"))
                .andExpect(jsonPath("$[0].plusOne.fullName").value("Bar Test"))
                .andExpect(jsonPath("$[1].token").value("123abc"))
                .andExpect(jsonPath("$[1].mainGuest.fullName").value("Test McTest"));
    }
}
