package br.unicamp.var.web.organizador;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.util.List;
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
 * Telas do organizador com MockMvc: painel, nova votação, acessos, gestão em cada estado, POSTs de
 * transição, 404, sigilo e controle de acesso. Os dados são criados no teste ({@code var.demo.ativo=false}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "organizador", roles = "ORGANIZADOR")
class OrganizadorControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GestaoVotacoes gestao;

    @Autowired
    private RepositorioVotacoes votacoes;

    /** Ids de eleitor únicos, para cada teste ter contas novas. */
    private static String prefixo() {
        return "t" + UUID.randomUUID().toString().substring(0, 6) + "-";
    }

    private String criar(String pergunta, String opcoes, Sigilo sigilo, String... eleitores) {
        FormularioVotacao f = new FormularioVotacao();
        f.setPergunta(pergunta);
        f.setOpcoes(opcoes);
        f.setSigilo(sigilo);
        f.setEleitores(String.join("\n", eleitores));
        return gestao.criar(f, "organizador").id();
    }

    private Votacao votacao(String id) {
        return votacoes.buscar(id).orElseThrow();
    }

    /** Votação com 3 eleitores; vota na ordem e leva até o status pedido. */
    private String votacaoEm(String status, Sigilo sigilo, String... votos) {
        String p = prefixo();
        String id = criar("Pergunta " + p, "Azul\nVerde\nBranca", sigilo, p + "1;Ana", p + "2;Bia", p + "3;Caio");
        if ("rascunho".equals(status)) {
            return id;
        }
        gestao.abrir(id, "organizador");
        for (int i = 0; i < votos.length; i++) {
            votacao(id).registrarVoto(p + (i + 1), votos[i]);
        }
        if (!"aberta".equals(status)) {
            gestao.encerrar(id, "organizador");
        }
        if ("apurada".equals(status)) {
            gestao.apurar(id, "organizador");
        }
        return id;
    }

    private String pagina(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Nested
    @DisplayName("Controle de acesso")
    class Acesso {

        @Test
        @WithMockUser(username = "e01", roles = "ELEITOR")
        @DisplayName("eleitor recebe 403 no painel, no formulário e nas ações")
        void eleitorProibido() throws Exception {
            String id = votacaoEm("rascunho", Sigilo.SECRETO);
            mvc.perform(get("/organizador")).andExpect(status().isForbidden());
            mvc.perform(get("/organizador/votacoes/nova")).andExpect(status().isForbidden());
            mvc.perform(get("/organizador/votacoes/" + id)).andExpect(status().isForbidden());
            mvc.perform(post("/organizador/votacoes/" + id + "/abrir").with(csrf())).andExpect(status().isForbidden());
            assertEquals("rascunho", votacao(id).status());
        }

        @Test
        @DisplayName("POST sem token CSRF é recusado")
        void semCsrf() throws Exception {
            String id = votacaoEm("rascunho", Sigilo.SECRETO);

            mvc.perform(post("/organizador/votacoes/" + id + "/abrir")).andExpect(status().isForbidden());
            assertEquals("rascunho", votacao(id).status());
        }
    }

    @Nested
    @DisplayName("Painel")
    class Painel {

        @Test
        @DisplayName("com votações mostra cards com participação e filtra por status")
        void comVotacoes() throws Exception {
            String aberta = votacaoEm("aberta", Sigilo.SECRETO, "Azul");
            String rascunho = votacaoEm("rascunho", Sigilo.SECRETO);

            String todas = pagina("/organizador");
            String filtrada = pagina("/organizador?status=aberta");

            assertEquals(true, todas.contains("/organizador/votacoes/" + aberta)
                    && todas.contains("/organizador/votacoes/" + rascunho));
            assertEquals(true, todas.contains("1 de 3 · 33,3%") && todas.contains("Votos registrados"));
            assertEquals(true, filtrada.contains("/organizador/votacoes/" + aberta));
            assertEquals(false, filtrada.contains("/organizador/votacoes/" + rascunho));
            assertEquals(true, filtrada.contains("aria-current=\"page\""));
        }
    }

    @Nested
    @DisplayName("Nova votação")
    class NovaVotacao {

        @Test
        @DisplayName("formulário abre com os padrões e as quatro seções")
        void formulario() throws Exception {
            mvc.perform(get("/organizador/votacoes/nova"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Pergunta e opções"), containsString("Regra de apuração"),
                            containsString("Sigilo e período"), containsString("Eleitores aptos"),
                            containsString("value=\"2/3\""), containsString("name=\"_csrf\""),
                            containsString("Usar eleitores de demonstração (e01–e12)"),
                            containsString("/js/organizador.js"))));
        }

        @Test
        @DisplayName("válida com eleitores novos: cria e leva aos códigos de acesso")
        void validaComCodigos() throws Exception {
            String p = prefixo();
            MvcResult criacao = mvc.perform(post("/organizador/votacoes/nova").with(csrf())
                            .param("pergunta", "Quem deve ser o presidente?")
                            .param("opcoes", "Azul\nVerde")
                            .param("tipoRegra", "ABSOLUTA")
                            .param("segundoTurno", "true")
                            .param("sigilo", "SECRETO")
                            .param("inicio", "2027-03-01T08:00")
                            .param("fim", "2027-03-01T18:00")
                            .param("eleitores", p + "1;Ana\n" + p + "2;Bia"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/organizador/votacoes/*/acessos"))
                    .andExpect(flash().attribute("alerta", Alerta.sucesso("Votação criada em rascunho.")))
                    .andExpect(flash().attribute("acessos", hasSize(2)))
                    .andReturn();

            String url = criacao.getResponse().getRedirectedUrl();
            mvc.perform(get(url).flashAttr("acessos", criacao.getFlashMap().get("acessos")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString(p + "1"), containsString("class=\"codigo"),
                            containsString("Imprimir"), containsString("Quem deve ser o presidente?"))));
            mvc.perform(get(url))
                    .andExpect(content().string(containsString("Nenhum código para mostrar")));
        }

        @Test
        @DisplayName("válida com eleitores que já têm conta: vai direto à gestão")
        void validaSemCodigos() throws Exception {
            String p = prefixo();
            votacaoEm("rascunho", Sigilo.SECRETO);
            criar("Primeira", "A\nB", Sigilo.SECRETO, p + "1;Ana");

            mvc.perform(post("/organizador/votacoes/nova").with(csrf())
                            .param("pergunta", "Segunda")
                            .param("opcoes", "A\nB")
                            .param("tipoRegra", "SIMPLES")
                            .param("sigilo", "IDENTIFICADO")
                            .param("eleitores", p + "1;Ana"))
                    .andExpect(redirectedUrlPattern("/organizador/votacoes/*"))
                    .andExpect(flash().attributeCount(1));
        }

        @Test
        @DisplayName("inválida: mostra os erros por campo e mantém o que foi digitado")
        void invalida() throws Exception {
            mvc.perform(post("/organizador/votacoes/nova").with(csrf())
                            .param("pergunta", "Minha pergunta digitada")
                            .param("opcoes", "Só uma opção")
                            .param("tipoRegra", "QUALIFICADA")
                            .param("quorum", "abc")
                            .param("fracaoExigida", "2/3")
                            .param("sigilo", "SECRETO")
                            .param("eleitores", "e01;Ana\nlinha ruim"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("organizador/nova"))
                    .andExpect(content().string(allOf(
                            containsString("Corrija os campos destacados"),
                            containsString("campo-com-erro"),
                            containsString("Informe pelo menos duas opções, uma por linha."),
                            containsString("Use uma fração (2/3) ou um percentual (50%)."),
                            containsString("Linha 2: use o formato id;nome"),
                            containsString("value=\"Minha pergunta digitada\""),
                            containsString("value=\"abc\""))));
        }

        @Test
        @DisplayName("data em formato inválido vira erro no campo, sem chamar o domínio")
        void dataInvalida() throws Exception {
            mvc.perform(post("/organizador/votacoes/nova").with(csrf())
                            .param("pergunta", "Pergunta")
                            .param("opcoes", "A\nB")
                            .param("inicio", "ontem")
                            .param("eleitores", "e01;Ana"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Data e hora inválidas.")));
        }

        @Test
        @DisplayName("botão de demonstração preenche e01 a e12 e mantém o resto")
        void usarDemonstracao() throws Exception {
            mvc.perform(post("/organizador/votacoes/nova").with(csrf())
                            .param("usarDemonstracao", "sim")
                            .param("pergunta", "Mantida")
                            .param("eleitores", ""))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("e01;"), containsString("e12;"),
                            containsString("value=\"Mantida\""), not(containsString("campo-com-erro")))));
        }
    }

    @Nested
    @DisplayName("Gestão da votação")
    class Gestao {

        @Test
        @DisplayName("rascunho: mostra só \"Abrir votação\", configuração, presença e auditoria")
        void rascunho() throws Exception {
            String html = pagina("/organizador/votacoes/" + votacaoEm("rascunho", Sigilo.SECRETO));

            assertEquals(List.of(true, false, false, true, true, true), List.of(
                    html.contains("Abrir votação"), html.contains("Encerrar votação"),
                    html.contains(">Apurar<"), html.contains("Votação criada"),
                    html.contains("class=\"linha-tempo\""), html.contains("Sem período")));
        }

        @Test
        @DisplayName("aberta: mostra \"Encerrar votação\" e só a participação")
        void aberta() throws Exception {
            String html = pagina("/organizador/votacoes/" + votacaoEm("aberta", Sigilo.SECRETO, "Azul", "Azul"));

            assertEquals(List.of(false, true, true, true, false), List.of(
                    html.contains("Abrir votação"), html.contains("Encerrar votação"),
                    html.contains("2 de 3 aptos votaram"), html.contains("Votou"), html.contains("Ver resultado")));
        }

        @Test
        @DisplayName("encerrada: mostra \"Apurar\"")
        void encerrada() throws Exception {
            String html = pagina("/organizador/votacoes/" + votacaoEm("encerrada", Sigilo.SECRETO, "Azul"));

            assertEquals(List.of(true, false), List.of(html.contains(">Apurar<"), html.contains("Encerrar votação")));
        }

        @Test
        @DisplayName("apurada com 2º turno: resultado, relatório e \"Criar 2º turno\"")
        void apuradaComSegundoTurno() throws Exception {
            String id = votacaoEm("apurada", Sigilo.SECRETO, "Azul", "Verde", "Branca");

            mvc.perform(get("/organizador/votacoes/" + id))
                    .andExpect(content().string(allOf(
                            containsString("/votacoes/" + id + "/resultado"),
                            containsString("/votacoes/" + id + "/relatorio"),
                            containsString("Criar 2º turno"),
                            containsString("Votação apurada"))));
        }

        @Test
        @DisplayName("votação secreta não exibe a escolha de ninguém")
        void secretaSemEscolha() throws Exception {
            String html = pagina("/organizador/votacoes/" + votacaoEm("apurada", Sigilo.SECRETO, "Verde"));

            assertEquals(false, html.contains(">Escolha<"));
            assertEquals(false, html.contains("<td>Verde</td>"));
        }

        @Test
        @DisplayName("votação identificada encerrada exibe a escolha de cada eleitor")
        void identificadaComEscolha() throws Exception {
            String html = pagina("/organizador/votacoes/" + votacaoEm("encerrada", Sigilo.IDENTIFICADO, "Verde"));

            assertEquals(true, html.contains(">Escolha<") && html.contains("<td>Verde</td>"));
        }

        @Test
        @DisplayName("votação inexistente dá 404 amigável")
        void inexistente() throws Exception {
            mvc.perform(get("/organizador/votacoes/nao-existe"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("Votação não encontrada")));
            mvc.perform(post("/organizador/votacoes/nao-existe/abrir").with(csrf()))
                    .andExpect(status().isNotFound());
            mvc.perform(get("/organizador/votacoes/nao-existe/acessos"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Ações (POST → redirect)")
    class Acoes {

        @Test
        @DisplayName("abrir, encerrar e apurar mudam o estado e registram o organizador")
        void ciclo() throws Exception {
            String id = votacaoEm("rascunho", Sigilo.SECRETO);
            String gestaoUrl = "/organizador/votacoes/" + id;

            mvc.perform(post(gestaoUrl + "/abrir").with(csrf()))
                    .andExpect(redirectedUrl(gestaoUrl))
                    .andExpect(flash().attribute("alerta",
                            Alerta.sucesso("Votação aberta. Os eleitores já podem votar.")));
            mvc.perform(post(gestaoUrl + "/encerrar").with(csrf()))
                    .andExpect(flash().attribute("alerta", Alerta.sucesso("Votação encerrada. Agora é só apurar.")));
            mvc.perform(post(gestaoUrl + "/apurar").with(csrf()))
                    .andExpect(flash().attribute("alerta", Alerta.sucesso("Votação apurada. Sem vencedor.")));

            assertEquals("apurada", votacao(id).status());
            assertEquals(List.of("organizador"), votacao(id).auditoria().stream()
                    .map(evento -> evento.autor()).distinct().toList());
        }

        @Test
        @DisplayName("operação no estado errado volta à gestão com alerta de erro")
        void estadoErrado() throws Exception {
            String id = votacaoEm("rascunho", Sigilo.SECRETO);

            mvc.perform(post("/organizador/votacoes/" + id + "/encerrar").with(csrf()))
                    .andExpect(redirectedUrl("/organizador/votacoes/" + id))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("Esta ação não é permitida com a votação em rascunho.")));
        }

        @Test
        @DisplayName("criar 2º turno leva à gestão da nova votação; de novo dá erro")
        void segundoTurno() throws Exception {
            String id = votacaoEm("apurada", Sigilo.SECRETO, "Azul", "Verde", "Branca");

            MvcResult resultado = mvc.perform(post("/organizador/votacoes/" + id + "/segundo-turno").with(csrf()))
                    .andExpect(redirectedUrlPattern("/organizador/votacoes/*"))
                    .andReturn();
            String novaUrl = resultado.getResponse().getRedirectedUrl();
            assertEquals(false, novaUrl.endsWith(id));

            mvc.perform(get(novaUrl)).andExpect(content().string(allOf(
                    containsString("2º turno"), containsString("Ver o 1º turno"))));
            mvc.perform(get("/organizador/votacoes/" + id)).andExpect(content().string(allOf(
                    containsString("Ir para o 2º turno"), not(containsString("Criar 2º turno")))));
            mvc.perform(post("/organizador/votacoes/" + id + "/segundo-turno").with(csrf()))
                    .andExpect(redirectedUrl("/organizador/votacoes/" + id))
                    .andExpect(flash().attribute("alerta", Alerta.erro("O 2º turno desta votação já foi criado.")));
        }
    }
}
