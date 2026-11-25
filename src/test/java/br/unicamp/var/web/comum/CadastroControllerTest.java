package br.unicamp.var.web.comum;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tela de cadastro ({@code /cadastro}) e os ajustes do login.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CadastroControllerTest {

    @Autowired
    private MockMvc mvc;

    private static MockHttpServletRequestBuilder cadastro(String usuario, String senha, String confirmar,
                                                          String papel) {
        return post("/cadastro").with(csrf()).param("nome", "Renata Alves").param("usuario", usuario)
                .param("senha", senha).param("confirmar", confirmar).param("papel", papel);
    }

    @Test
    @DisplayName("/cadastro é público e mostra o formulário com os dois tipos de conta")
    void formulario() throws Exception {
        mvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<title>Criar conta · VAR</title>"),
                        containsString("value=\"ORGANIZADOR\""), containsString("value=\"ELEITOR\""),
                        containsString("name=\"_csrf\""), containsString("Repita a senha"))));
    }

    @Test
    @WithMockUser(username = "e01", roles = "ELEITOR")
    @DisplayName("quem já está logado volta ao início")
    void logado() throws Exception {
        mvc.perform(get("/cadastro")).andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("cadastro válido leva ao login com o usuário preenchido, e a conta nova entra")
    void cadastroValido() throws Exception {
        mvc.perform(cadastro("renata.org", "senhaforte", "senhaforte", "ORGANIZADOR"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("usuarioCriado", "renata.org"))
                .andExpect(flash().attribute("alerta",
                        Alerta.sucesso("Conta criada. Entre com o seu usuário e a sua senha.")));

        mvc.perform(get("/login").flashAttr("usuarioCriado", "renata.org"))
                .andExpect(content().string(containsString("value=\"renata.org\"")));
        mvc.perform(formLogin("/login").user("renata.org").password("senhaforte"))
                .andExpect(authenticated().withRoles("ORGANIZADOR"));
    }

    @Test
    @DisplayName("com erros, volta ao formulário com as mensagens e sem devolver a senha")
    void comErros() throws Exception {
        mvc.perform(cadastro("renata2", "senhaforte", "outrasenha", "ELEITOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(content().string(allOf(
                        containsString("As senhas não são iguais."), containsString("value=\"renata2\""),
                        containsString("Corrija os campos destacados"), not(containsString("senhaforte")))));
    }

    @Test
    @DisplayName("POST sem token CSRF é recusado")
    void semCsrf() throws Exception {
        mvc.perform(post("/cadastro").param("usuario", "x")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("o login tem o link \"Criar conta\" e não tem mais a frase dos dados fictícios")
    void login() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/cadastro\""), containsString("Criar conta"),
                        not(containsString("Dados fictícios, recriados a cada reinício.")))));
    }
}
