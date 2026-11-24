package br.unicamp.var.web.comum;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fundação da aplicação web: contexto, páginas públicas, autorização por papel (RNF-05), layout e
 * alerta flash. Roda com {@code var.demo.ativo=false} (properties de teste).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AplicacaoWebTest {

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("Páginas públicas")
    class Publicas {

        @Test
        @DisplayName("/login responde 200 com o formulário e o token CSRF")
        void login() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(content().string(allOf(
                            containsString("<title>Entrar · VAR</title>"),
                            containsString("name=\"_csrf\""),
                            containsString("name=\"username\""),
                            containsString("/css/var.css"),
                            containsString("/favicon.svg"))));
        }

        @Test
        @DisplayName("/login?error mostra o erro de credenciais")
        void loginComErro() throws Exception {
            mvc.perform(get("/login").param("error", ""))
                    .andExpect(content().string(containsString("Usuário ou senha incorretos")));
        }

        @Test
        @DisplayName("/login?saiu confirma a saída")
        void loginDepoisDeSair() throws Exception {
            mvc.perform(get("/login").param("saiu", ""))
                    .andExpect(content().string(containsString("Você saiu da sua conta")));
        }

        @Test
        @DisplayName("/estilo responde 200, com CSS próprio e os componentes do contrato")
        void estilo() throws Exception {
            mvc.perform(get("/estilo"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("<title>Guia de estilo · VAR</title>"),
                            containsString("/css/estilo.css"),
                            containsString("badge badge-encerrada"),
                            containsString("class=\"linha-tempo\""),
                            containsString("58,3%"))));
        }

        @Test
        @DisplayName("/ anônimo vê a página inicial pública, com o botão Entrar")
        void inicioAnonimo() throws Exception {
            mvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("inicio"))
                    .andExpect(content().string(allOf(containsString("href=\"/login\""), not(containsString("Sair")))));
        }

        @Test
        @DisplayName("recursos estáticos são públicos")
        void estaticos() throws Exception {
            mvc.perform(get("/css/var.css")).andExpect(status().isOk());
            mvc.perform(get("/img/logo-var.svg")).andExpect(status().isOk());
            mvc.perform(get("/favicon.svg")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Autorização por papel")
    class Autorizacao {

        @Test
        @DisplayName("/organizador sem login redireciona para o login")
        void organizadorSemLogin() throws Exception {
            mvc.perform(get("/organizador")).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("/votacoes/** e /premiacoes/** exigem login")
        void rotasAutenticadas() throws Exception {
            mvc.perform(get("/votacoes/x/resultado")).andExpect(redirectedUrlPattern("**/login"));
            mvc.perform(get("/premiacoes/x")).andExpect(redirectedUrlPattern("**/login"));
        }

        @Test
        @WithMockUser(username = "e01", roles = "ELEITOR")
        @DisplayName("eleitor em /organizador recebe 403")
        void eleitorNoOrganizador() throws Exception {
            mvc.perform(get("/organizador")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("organizador em /eleitor recebe 403")
        void organizadorNoEleitor() throws Exception {
            mvc.perform(get("/eleitor")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("/ redireciona o organizador para /organizador")
        void inicioOrganizador() throws Exception {
            mvc.perform(get("/")).andExpect(redirectedUrl("/organizador"));
        }

        @Test
        @WithMockUser(username = "e01", roles = "ELEITOR")
        @DisplayName("/ redireciona o eleitor para /eleitor")
        void inicioEleitor() throws Exception {
            mvc.perform(get("/")).andExpect(redirectedUrl("/eleitor"));
        }

        @Test
        @WithMockUser(username = "visitante", roles = "OUTRO")
        @DisplayName("/ com usuário sem papel conhecido mostra a página inicial")
        void inicioSemPapel() throws Exception {
            mvc.perform(get("/")).andExpect(view().name("inicio"));
        }
    }

    @Nested
    @DisplayName("Layout")
    class Layout {

        @Test
        @WithMockUser(username = "e07", roles = "ELEITOR")
        @DisplayName("eleitor vê a navegação dele, o login como nome (sem conta) e o botão Sair")
        void navegacaoEleitor() throws Exception {
            mvc.perform(get("/estilo"))
                    .andExpect(content().string(allOf(
                            containsString("Minhas votações"),
                            containsString("Verificar comprovante"),
                            not(containsString(">Painel<")),
                            containsString(">e07<"),
                            containsString(">Sair<"))));
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("organizador vê o Painel e não a navegação do eleitor")
        void navegacaoOrganizador() throws Exception {
            mvc.perform(get("/estilo"))
                    .andExpect(content().string(allOf(
                            containsString(">Painel<"), not(containsString("Minhas votações")))));
        }

        @Test
        @DisplayName("o alerta flash aparece no topo com a classe do tipo")
        void alertaFlash() throws Exception {
            mvc.perform(get("/estilo").flashAttr("alerta", Alerta.erro("Você já votou nesta votação.")))
                    .andExpect(content().string(allOf(
                            containsString("class=\"alerta alerta-erro\""),
                            containsString("role=\"alert\""))));
            mvc.perform(get("/estilo").flashAttr("alerta", Alerta.sucesso("Votação aberta.")))
                    .andExpect(content().string(containsString("class=\"alerta alerta-sucesso\" role=\"status\"")));
        }

        @Test
        @DisplayName("rodapé com o nome do projeto")
        void rodape() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(content().string(
                            containsString("VAR — Votação, Apuração e Resultados · MC656 Unicamp")));
        }
    }

    @Nested
    @DisplayName("Logout e CSRF")
    class Logout {

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("logout por POST com CSRF volta ao login")
        void logoutComCsrf() throws Exception {
            mvc.perform(post("/logout").with(csrf())).andExpect(redirectedUrl("/login?saiu"));
        }

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("POST sem token CSRF é recusado")
        void postSemCsrf() throws Exception {
            mvc.perform(post("/logout")).andExpect(status().isForbidden());
        }
    }
}
