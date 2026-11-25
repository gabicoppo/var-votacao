package br.unicamp.var.web.comum;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Página inicial pública e páginas de erro, sem demonstração ({@code var.demo.ativo=false}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaginasPublicasTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("página inicial: hero, 3 contextos, como funciona, ODS 16 e botão Entrar")
    void inicio() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Votação, apuração e resultados com transparência para o futebol"),
                        containsString("Eleição presidencial"),
                        containsString("Craque do Campeonato"),
                        containsString("Assembleia do conselho"),
                        containsString("Como funciona"),
                        containsString("ODS 16"),
                        containsString(">Entrar</a>"))));
    }

    @Test
    @DisplayName("sem demonstração, o login não mostra as contas de demonstração")
    void loginSemDemonstracao() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(not(containsString("Contas de demonstração"))));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"403, Acesso negado", "404, Página não encontrada", "500, Algo deu errado"})
    @DisplayName("páginas de erro renderizam no layout, com botão Voltar ao início")
    void paginasDeErro(int codigo, String titulo) throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr("jakarta.servlet.error.status_code", codigo)
                        .requestAttr("jakarta.servlet.error.request_uri", "/qualquer"))
                .andExpect(status().is(codigo))
                .andExpect(content().string(allOf(
                        containsString("<title>" + titulo + " · VAR</title>"),
                        containsString("Voltar ao início"),
                        containsString("class=\"topo\""))));
    }
}
