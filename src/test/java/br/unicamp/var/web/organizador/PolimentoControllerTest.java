package br.unicamp.var.web.organizador;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.auditoria.TipoEvento;
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

/**
 * Rotas e telas da Fase 3 com MockMvc: presença em JSON, descrição da regra, alterar regra,
 * duplicar, convite, telão e códigos de acesso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "organizador", roles = "ORGANIZADOR")
class PolimentoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GestaoVotacoes gestao;

    @Autowired
    private RepositorioVotacoes votacoes;

    private String p;

    private String criar(Sigilo sigilo) {
        p = "p" + UUID.randomUUID().toString().substring(0, 6) + "-";
        FormularioVotacao f = new FormularioVotacao();
        f.setPergunta("Pergunta " + p);
        f.setOpcoes("Azul\nVerde");
        f.setSigilo(sigilo);
        f.setEleitores(p + "1;Ana\n" + p + "2;Bia\n" + p + "3;Caio");
        return gestao.criar(f, "organizador").id();
    }

    private Votacao votacao(String id) {
        return votacoes.buscar(id).orElseThrow();
    }

    private String pagina(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Nested
    @DisplayName("presenca.json")
    class PresencaJsonRota {

        @Test
        @DisplayName("devolve os ids de quem votou e nunca as opções")
        void soIds() throws Exception {
            String id = criar(Sigilo.IDENTIFICADO);
            gestao.abrir(id, "organizador");
            votacao(id).registrarVoto(p + "2", "Verde");

            String json = mvc.perform(get("/organizador/votacoes/" + id + "/presenca.json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.votaram[0]").value(p + "2"))
                    .andExpect(jsonPath("$.votaram.length()").value(1))
                    .andReturn().getResponse().getContentAsString();

            assertEquals(false, json.contains("Verde") || json.contains("Azul"));
        }

        @Test
        @WithMockUser(username = "e01", roles = "ELEITOR")
        @DisplayName("eleitor recebe 403")
        void eleitorProibido() throws Exception {
            mvc.perform(get("/organizador/votacoes/" + criar(Sigilo.SECRETO) + "/presenca.json"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("votação inexistente dá 404")
        void inexistente() throws Exception {
            mvc.perform(get("/organizador/votacoes/nao-existe/presenca.json")).andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("descricao-regra devolve o mesmo texto de descricao() da regra")
    void descricaoRegra() throws Exception {
        mvc.perform(get("/organizador/votacoes/descricao-regra")
                        .param("tipoRegra", "QUALIFICADA").param("quorum", "50").param("fracaoExigida", "2/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value(
                        "Maioria qualificada: quórum de 50% dos aptos e pelo menos 66,7% dos votos válidos."));
    }

    @Nested
    @DisplayName("Alterar regra")
    class AlterarRegra {

        @Test
        @DisplayName("aparece só no rascunho")
        void soNoRascunho() throws Exception {
            String id = criar(Sigilo.SECRETO);
            assertEquals(true, pagina("/organizador/votacoes/" + id).contains("Alterar regra de apuração"));

            gestao.abrir(id, "organizador");
            assertEquals(false, pagina("/organizador/votacoes/" + id).contains("Alterar regra de apuração"));
        }

        @Test
        @DisplayName("no rascunho, troca a regra e registra na auditoria")
        void troca() throws Exception {
            String id = criar(Sigilo.SECRETO);

            mvc.perform(post("/organizador/votacoes/" + id + "/regra").with(csrf()).param("tipoRegra", "SIMPLES"))
                    .andExpect(redirectedUrl("/organizador/votacoes/" + id))
                    .andExpect(flash().attribute("alerta", Alerta.sucesso(
                            "Regra alterada: Maioria simples: vence a opção mais votada; empate fica sem vencedor.")));

            assertEquals(true, votacao(id).auditoria().stream()
                    .anyMatch(evento -> evento.tipo() == TipoEvento.REGRA_ALTERADA));
            assertEquals(true, pagina("/organizador/votacoes/" + id).contains("Regra alterada"));
        }

        @Test
        @DisplayName("com campos inválidos, mostra a gestão com o erro e o painel aberto")
        void invalida() throws Exception {
            String id = criar(Sigilo.SECRETO);

            mvc.perform(post("/organizador/votacoes/" + id + "/regra").with(csrf())
                            .param("tipoRegra", "QUALIFICADA").param("quorum", "abc").param("fracaoExigida", "2/3"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("organizador/gestao"))
                    .andExpect(content().string(allOf(containsString("Corrija a regra"),
                            containsString("Use uma fração (2/3) ou um percentual (50%)."),
                            containsString("<details class=\"card alterar-regra nao-imprimir\" open"))));
        }

        @Test
        @DisplayName("com a votação aberta, a rota recusa com mensagem amigável")
        void abertaRecusa() throws Exception {
            String id = criar(Sigilo.SECRETO);
            gestao.abrir(id, "organizador");

            mvc.perform(post("/organizador/votacoes/" + id + "/regra").with(csrf()).param("tipoRegra", "SIMPLES"))
                    .andExpect(redirectedUrl("/organizador/votacoes/" + id))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("Esta ação não é permitida com a votação aberta.")));
        }
    }

    @Test
    @DisplayName("duplicar abre o formulário preenchido e sem período")
    void duplicar() throws Exception {
        String id = criar(Sigilo.IDENTIFICADO);

        mvc.perform(get("/organizador/votacoes/" + id + "/duplicar"))
                .andExpect(status().isOk())
                .andExpect(view().name("organizador/nova"))
                .andExpect(content().string(allOf(
                        // O Thymeleaf escapa o "ó" dentro de atributos.
                        containsString("value=\"Pergunta " + p + " (c&oacute;pia)\""),
                        containsString(p + "1;Ana"),
                        containsString("Cópia pronta"),
                        containsString("name=\"inicio\" value=\"\""),
                        containsString("value=\"IDENTIFICADO\" id=\"sigilo2\" name=\"sigilo\" checked=\"checked\""))));
    }

    @Nested
    @DisplayName("Gestão")
    class Gestao {

        @Test
        @DisplayName("aberta: convite com o link absoluto da cédula, telão e atualização ao vivo")
        void aberta() throws Exception {
            String id = criar(Sigilo.SECRETO);
            gestao.abrir(id, "organizador");

            mvc.perform(get("/organizador/votacoes/" + id))
                    .andExpect(content().string(allOf(
                            containsString("Convide os eleitores"),
                            containsString("value=\"http://localhost/eleitor/votacoes/" + id + "\""),
                            containsString("Entre com seu login e código de acesso."),
                            containsString("/votacoes/" + id + "/telao"),
                            containsString("data-gestao-ao-vivo=\"sim\""),
                            containsString("/votacoes/" + id + "/andamento.json"),
                            containsString("/organizador/votacoes/" + id + "/presenca.json"),
                            containsString("aria-live=\"polite\""),
                            containsString("Buscar eleitor"))));
        }

        @Test
        @DisplayName("rascunho: convite e duplicar, sem telão nem ao vivo")
        void rascunho() throws Exception {
            String id = criar(Sigilo.SECRETO);

            mvc.perform(get("/organizador/votacoes/" + id))
                    .andExpect(content().string(allOf(
                            containsString("Convide os eleitores"),
                            containsString("/organizador/votacoes/" + id + "/duplicar"),
                            not(containsString("/telao")),
                            not(containsString("data-gestao-ao-vivo")))));
        }

        @Test
        @DisplayName("apurada: telão, sem convite")
        void apurada() throws Exception {
            String id = criar(Sigilo.SECRETO);
            gestao.abrir(id, "organizador");
            gestao.encerrar(id, "organizador");
            gestao.apurar(id, "organizador");

            mvc.perform(get("/organizador/votacoes/" + id))
                    .andExpect(content().string(allOf(
                            containsString("/votacoes/" + id + "/telao"),
                            not(containsString("Convide os eleitores")),
                            containsString("Não votou"))));
        }
    }

    @Test
    @DisplayName("acessos: aviso de que não serão mostrados de novo e botão de CSV")
    void acessos() throws Exception {
        String id = criar(Sigilo.SECRETO);
        List<AcessoEleitor> acessos = List.of(new AcessoEleitor("novo", "Nova Pessoa", "ABCD2345"));

        mvc.perform(get("/organizador/votacoes/" + id + "/acessos").flashAttr("acessos", acessos))
                .andExpect(content().string(allOf(
                        containsString("Estes códigos não serão mostrados de novo."),
                        containsString("data-baixar-csv"),
                        containsString("codigos-acesso-" + id.substring(0, 8) + ".csv"),
                        containsString("data-tabela-acessos"))));
    }

    @Test
    @DisplayName("formulário de nova votação traz o resumo e as prévias de data")
    void formularioComResumo() throws Exception {
        mvc.perform(get("/organizador/votacoes/nova"))
                .andExpect(content().string(allOf(
                        containsString("data-resumo"),
                        containsString("data-previa-de=\"inicio\""),
                        containsString("data-obrigatorio=\"Informe a pergunta da votação.\""),
                        containsString("/organizador/votacoes/descricao-regra"))))
                .andExpect(content().string(startsWith("<!DOCTYPE html>")));
    }
}
