package br.unicamp.var.web.premiacao;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.top3.StatusPremiacao;
import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Telas do organizador para premiações (história #23): formulário (válido, inválido e exemplo),
 * códigos de acesso, gestão e transições do ciclo, 404 e acesso negado ao eleitor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "organizador", roles = "ORGANIZADOR")
class PremiacaoOrganizadorControllerTest {

    private static final String BASE = "/organizador/premiacoes/";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    private PremiacaoCadastrada cadastrar() {
        return premiacoes.salvar(new PremiacaoTop3("Bola de Ouro", "Regularidade", List.of("Ana", "Bia", "Caio"),
                List.of(new CategoriaEleitor("Jornalistas", 2), new CategoriaEleitor("Torcedores", 1.5)),
                Map.of("e01", "Jornalistas", "e02", "Torcedores")));
    }

    @Nested
    @DisplayName("Nova premiação")
    class Nova {

        @Test
        @DisplayName("Formulário vazio com as quatro seções e o botão de exemplo")
        void formulario() throws Exception {
            mvc.perform(get(BASE + "nova"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("premiacao/nova"))
                    .andExpect(content().string(allOf(
                            containsString("<title>Nova premiação · VAR</title>"),
                            containsString("name=\"titulo\""), containsString("name=\"candidatos\""),
                            containsString("name=\"categorias\""), containsString("name=\"eleitores\""),
                            containsString("name=\"preencherExemplo\""), containsString("name=\"_csrf\""),
                            containsString("/css/premiacao.css"))));
        }

        @Test
        @DisplayName("Preencher exemplo coloca o cenário fictício sem criar nada")
        void exemplo() throws Exception {
            int antes = premiacoes.listar().size();

            mvc.perform(post(BASE + "nova").param("preencherExemplo", "sim").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Craque da Final 2026"),
                            containsString("Jornalistas;2"), containsString("e03;Eleitor e03;Jornalistas"),
                            containsString("e12;Eleitor e12;Torcedores"), containsString("Tiago Paz"),
                            containsString("Exemplo preenchido"))));
            assertEquals(antes, premiacoes.listar().size());
        }

        @Test
        @DisplayName("Formulário inválido volta com o erro de cada campo e o que foi digitado")
        void invalido() throws Exception {
            mvc.perform(post(BASE + "nova").with(csrf())
                            .param("titulo", "")
                            .param("candidatos", "Ana\nBia")
                            .param("categorias", "Jornalistas;0")
                            .param("eleitores", "e01;Ana"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("premiacao/nova"))
                    .andExpect(content().string(allOf(
                            containsString("Corrija os campos destacados"),
                            containsString("Informe o título da premiação."),
                            containsString("Informe pelo menos três candidatos"),
                            containsString("o peso deve ser maior que zero"),
                            containsString("use o formato id;nome;grupo"),
                            containsString("campo-com-erro"),
                            containsString("Jornalistas;0"))));
        }

        @Test
        @DisplayName("Válida com eleitor novo: cria em rascunho e mostra o código uma única vez")
        void criaComCodigos() throws Exception {
            String novo = "p" + UUID.randomUUID().toString().substring(0, 8);
            MvcResult resultado = mvc.perform(post(BASE + "nova").with(csrf())
                            .param("titulo", "Gol mais bonito")
                            .param("candidatos", "Gol A\nGol B\nGol C")
                            .param("categorias", "Torcedores;1")
                            .param("eleitores", novo + ";Pessoa Nova;Torcedores"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("alerta", Alerta.sucesso("Premiação criada em rascunho.")))
                    .andExpect(flash().attribute("acessos", hasSize(1)))
                    .andReturn();
            String destino = resultado.getResponse().getRedirectedUrl();
            String id = destino.substring(BASE.length(), destino.length() - "/acessos".length());
            assertEquals(StatusPremiacao.RASCUNHO, premiacoes.buscar(id).orElseThrow().premiacao().status());

            mvc.perform(get(destino).flashAttr("acessos", resultado.getFlashMap().get("acessos")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Códigos de acesso"),
                            containsString("Gol mais bonito"), containsString(novo), containsString("Pessoa Nova"),
                            containsString("codigo codigo-pequeno"), containsString("data-imprimir"))));
            mvc.perform(get(destino))
                    .andExpect(content().string(containsString("Nenhum código para mostrar")));
        }

        @Test
        @DisplayName("Válida sem conta nova: vai direto para a gestão")
        void criaSemCodigos() throws Exception {
            String id = "q" + UUID.randomUUID().toString().substring(0, 8);
            String eleitor = id + ";Pessoa;Torcedores";
            mvc.perform(post(BASE + "nova").with(csrf()).param("titulo", "Primeira").param("candidatos", "A\nB\nC")
                    .param("categorias", "Torcedores;1").param("eleitores", eleitor));

            MvcResult resultado = mvc.perform(post(BASE + "nova").with(csrf()).param("titulo", "Segunda")
                            .param("candidatos", "A\nB\nC").param("categorias", "Torcedores;1")
                            .param("eleitores", eleitor))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeCount(1))
                    .andReturn();

            String destino = resultado.getResponse().getRedirectedUrl();
            assertTrue(destino.startsWith(BASE));
            assertFalse(destino.endsWith("/acessos"));
        }

        @Test
        @DisplayName("POST sem token CSRF é recusado")
        void semCsrf() throws Exception {
            mvc.perform(post(BASE + "nova").param("titulo", "X")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Gestão e ciclo")
    class Gestao {

        @Test
        @DisplayName("Rascunho: linha do tempo, grupos, pesos, participação e botão de abrir")
        void rascunho() throws Exception {
            PremiacaoCadastrada cadastro = cadastrar();

            mvc.perform(get(BASE + cadastro.id()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("premiacao/gestao"))
                    .andExpect(content().string(allOf(
                            containsString("Bola de Ouro"), containsString("Regularidade"),
                            containsString("badge-rascunho"), containsString("class=\"linha-tempo\""),
                            containsString("× 2"), containsString("× 1,5"),
                            containsString("0</strong> de <strong>2</strong>"),
                            containsString("Abrir premiação"), containsString("data-confirmar"),
                            containsString("Candidatos (3)"),
                            not(containsString("Apurar e publicar ranking")))));
        }

        @Test
        @DisplayName("Abrir, encerrar e apurar, com alerta de sucesso, até o botão Ver ranking")
        void cicloCompleto() throws Exception {
            PremiacaoCadastrada cadastro = cadastrar();
            String gestao = BASE + cadastro.id();

            mvc.perform(post(gestao + "/abrir").with(csrf()))
                    .andExpect(redirectedUrl(gestao))
                    .andExpect(flash().attribute("alerta",
                            Alerta.sucesso("Premiação aberta. Os eleitores aptos já podem votar.")));
            mvc.perform(get(gestao)).andExpect(content().string(containsString("Encerrar premiação")));
            mvc.perform(post(gestao + "/encerrar").with(csrf()))
                    .andExpect(flash().attribute("alerta", Alerta.sucesso("Premiação encerrada. Agora é só apurar.")));
            mvc.perform(get(gestao)).andExpect(content().string(containsString("Apurar e publicar ranking")));
            mvc.perform(post(gestao + "/apurar").with(csrf()))
                    .andExpect(flash().attribute("alerta",
                            Alerta.sucesso("Premiação apurada. O ranking já está publicado.")));

            assertEquals(StatusPremiacao.APURADA, cadastro.premiacao().status());
            mvc.perform(get(gestao))
                    .andExpect(content().string(allOf(containsString("Ranking publicado"),
                            containsString("href=\"/premiacoes/" + cadastro.id() + "/ranking\""),
                            containsString("badge-apurada"))));
        }

        @Test
        @DisplayName("Ação no estado errado volta com alerta de erro e não muda nada")
        void estadoErrado() throws Exception {
            PremiacaoCadastrada cadastro = cadastrar();

            mvc.perform(post(BASE + cadastro.id() + "/encerrar").with(csrf()))
                    .andExpect(redirectedUrl(BASE + cadastro.id()))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("Esta ação não é permitida no estado atual da premiação.")));
            mvc.perform(post(BASE + cadastro.id() + "/apurar").with(csrf()))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("Esta ação não é permitida no estado atual da premiação.")));
            assertEquals(StatusPremiacao.RASCUNHO, cadastro.premiacao().status());
        }

        @Test
        @DisplayName("Premiação inexistente dá 404 na gestão, nos acessos e nas ações")
        void inexistente() throws Exception {
            mvc.perform(get(BASE + "nao-existe"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("Premiação não encontrada")));
            mvc.perform(get(BASE + "nao-existe/acessos")).andExpect(status().isNotFound());
            mvc.perform(post(BASE + "nao-existe/abrir").with(csrf())).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("O modelo da gestão traz a etapa em minúsculas para a linha do tempo")
        void etapa() throws Exception {
            PremiacaoCadastrada cadastro = cadastrar();
            cadastro.premiacao().abrir();

            mvc.perform(get(BASE + cadastro.id()))
                    .andExpect(model().attributeExists("premiacao"))
                    .andExpect(content().string(containsString("aria-current=\"step\" class=\"ativo\">Aberta</li>")));
        }
    }

    @Nested
    @DisplayName("Acesso")
    class Acesso {

        @Test
        @WithMockUser(username = "e01", roles = "ELEITOR")
        @DisplayName("Eleitor recebe 403 em /organizador/premiacoes/**")
        void eleitor() throws Exception {
            mvc.perform(get(BASE + "nova")).andExpect(status().isForbidden());
            mvc.perform(get(BASE + cadastrar().id())).andExpect(status().isForbidden());
        }
    }
}
