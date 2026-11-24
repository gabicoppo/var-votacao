package br.unicamp.var.web.comum;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Login com as contas de demonstração ({@code var.demo.ativo=true} só neste teste).
 */
@SpringBootTest(properties = "var.demo.ativo=true")
@AutoConfigureMockMvc
class LoginDemonstracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ContasUsuarios contas;

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    @Test
    @DisplayName("os dados de demonstração são carregados no início")
    void dadosCarregados() {
        org.junit.jupiter.api.Assertions.assertEquals(4, votacoes.listar().size());
        org.junit.jupiter.api.Assertions.assertEquals(1, premiacoes.listar().size());
    }

    @Test
    @DisplayName("o login mostra o quadro de contas de demonstração")
    void quadroDeContas() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(containsString("Contas de demonstração")));
    }

    @Test
    @DisplayName("organizador/var2026 entra como ORGANIZADOR e passa por /")
    void loginOrganizador() throws Exception {
        mvc.perform(formLogin("/login").user("organizador").password("var2026"))
                .andExpect(authenticated().withUsername("organizador").withRoles("ORGANIZADOR"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("e01/1234 entra como ELEITOR")
    void loginEleitor() throws Exception {
        mvc.perform(formLogin("/login").user("e01").password("1234"))
                .andExpect(authenticated().withUsername("e01").withRoles("ELEITOR"));
    }

    @Test
    @DisplayName("senha errada volta ao login com erro")
    void senhaErrada() throws Exception {
        mvc.perform(formLogin("/login").user("organizador").password("errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("as contas de demonstração têm nomes fictícios")
    void nomesDemonstracao() {
        org.junit.jupiter.api.Assertions.assertEquals("Ana Ribeiro", contas.nomeDe("e01").orElseThrow());
        org.junit.jupiter.api.Assertions.assertEquals("Lucas Barreto", contas.nomeDe("e12").orElseThrow());
        org.junit.jupiter.api.Assertions.assertTrue(contas.nomeDe("e13").isEmpty());
    }
}
