package br.unicamp.var.web.comum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Health check do deploy: {@code /actuator/health} é público e responde UP; nenhum outro endpoint do
 * actuator fica exposto.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthCheckTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("/actuator/health responde UP sem login e sem detalhes")
    void healthPublico() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
    @DisplayName("outros endpoints do actuator não estão expostos, nem para o organizador")
    void outrosEndpointsFechados() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }
}
