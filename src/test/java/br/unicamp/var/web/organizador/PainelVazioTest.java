package br.unicamp.var.web.organizador;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Painel do organizador sem nenhuma votação nem premiação. Usa um contexto novo, porque os
 * repositórios em memória são compartilhados pelos outros testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PainelVazioTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
    @DisplayName("sem votações mostra o estado vazio e os botões de criar")
    void vazio() throws Exception {
        mvc.perform(get("/organizador"))
                .andExpect(status().isOk())
                .andExpect(view().name("organizador/painel"))
                .andExpect(content().string(allOf(
                        containsString("<title>Painel do organizador · VAR</title>"),
                        containsString("Nenhuma votação ainda"),
                        containsString("Nenhuma premiação ainda"),
                        containsString("/organizador/premiacoes/nova"),
                        containsString("/organizador/votacoes/nova"))));
    }
}
