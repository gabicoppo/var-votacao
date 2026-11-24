package br.unicamp.var.web.comum;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Perfil {@code prod} (deploy no Render): a senha do organizador vem de {@code VAR_SENHA_ORGANIZADOR},
 * não é a padrão e não aparece no login; os eleitores de demonstração continuam com {@code 1234}.
 */
@SpringBootTest(properties = "VAR_SENHA_ORGANIZADOR=senha-do-deploy")
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class PerfilProducaoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("o login mostra as contas de demonstração, mas não a senha do organizador")
    void loginSemSenhaDoOrganizador() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Contas de demonstração"), containsString("<code>1234</code>"),
                        not(containsString("senha-do-deploy")), not(containsString("var2026")))));
    }

    @Test
    @DisplayName("o organizador entra com a senha da variável de ambiente, e não com a padrão")
    void senhaDoOrganizador() throws Exception {
        mvc.perform(formLogin("/login").user("organizador").password("var2026")).andExpect(unauthenticated());
        mvc.perform(formLogin("/login").user("organizador").password("senha-do-deploy"))
                .andExpect(authenticated().withRoles("ORGANIZADOR"));
        mvc.perform(formLogin("/login").user("e01").password("1234")).andExpect(authenticated());
    }

    @Test
    @DisplayName("o health check continua público")
    void health() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
