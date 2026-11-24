package br.unicamp.var.web.eleitor;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.urna.Sigilo;
import br.unicamp.var.web.comum.Alerta;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
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
 * Voto do eleitor na web (histórias #26 e #28; RNF-01, RNF-02, RNF-04, RNF-05): cédula, revisão,
 * comprovante, casos de erro e sigilo. Os dados são criados em cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EleitorWebTest {

    private static final String AZUL = "Chapa Azul Marinho";
    private static final String VERDE = "Chapa Verde Musgo";
    private static final Alerta OPCAO_INEXISTENTE = Alerta.erro("A opção escolhida não existe nesta votação.");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private Clock relogio;

    /** Votação com os eleitores informados; a pergunta não contém o nome das opções. */
    private Votacao.Builder votacao(String... eleitores) {
        return Votacao.nova()
                .pergunta("Quem deve presidir o clube?")
                .opcoes(List.of(AZUL, VERDE))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(Arrays.stream(eleitores)
                        .map(id -> new Eleitor(id, "Eleitor " + id)).toList()))
                .relogio(relogio);
    }

    private Votacao aberta(Votacao.Builder builder) {
        Votacao votacao = builder.build();
        votacao.abrir();
        return votacoes.salvar(votacao);
    }

    private String url(Votacao votacao, String sufixo) {
        return "/eleitor/votacoes/" + votacao.id() + sufixo;
    }

    @Nested
    @DisplayName("Cédula e revisão")
    class Cedula {

        @Test
        @WithMockUser(username = "c01", roles = "ELEITOR")
        @DisplayName("apto vê as opções em cartões, o aviso de voto secreto e o passo 1 de 3")
        void aptoVeOpcoes() throws Exception {
            Votacao votacao = aberta(votacao("c01"));

            mvc.perform(get(url(votacao, "")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("eleitor/cedula"))
                    .andExpect(content().string(allOf(
                            containsString("class=\"opcao-radio\""), containsString(AZUL), containsString(VERDE),
                            containsString("Seu voto é secreto: ninguém, nem o organizador, verá sua escolha."),
                            containsString("Revisar voto"), containsString("sem prazo definido"))));
        }

        @Test
        @WithMockUser(username = "c02", roles = "ELEITOR")
        @DisplayName("votação identificada avisa que o organizador verá a escolha")
        void identificada() throws Exception {
            Votacao votacao = aberta(votacao("c02").sigilo(Sigilo.IDENTIFICADO));

            mvc.perform(get(url(votacao, "")))
                    .andExpect(content().string(
                            containsString("Votação identificada: o organizador verá sua escolha.")));
        }

        @Test
        @WithMockUser(username = "fora", roles = "ELEITOR")
        @DisplayName("não apto recebe 403 com a página amigável")
        void naoApto() throws Exception {
            Votacao votacao = aberta(votacao("c03"));

            mvc.perform(get(url(votacao, "")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(
                            containsString("Você não está na lista de eleitores desta votação")));
            mvc.perform(post(url(votacao, "/voto")).param("opcao", AZUL).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "c04", roles = "ELEITOR")
        @DisplayName("votação inexistente dá 404")
        void inexistente() throws Exception {
            mvc.perform(get("/eleitor/votacoes/nao-existe")).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "c05", roles = "ELEITOR")
        @DisplayName("revisar sem opção (ou com opção inválida) volta à cédula com erro")
        void revisarSemOpcao() throws Exception {
            Votacao votacao = aberta(votacao("c05"));

            mvc.perform(post(url(votacao, "/revisar")).with(csrf()))
                    .andExpect(view().name("eleitor/cedula"))
                    .andExpect(content().string(containsString("Escolha uma opção para continuar.")));
            mvc.perform(post(url(votacao, "/revisar")).param("opcao", "Chapa Fantasma").with(csrf()))
                    .andExpect(view().name("eleitor/cedula"));
        }

        @Test
        @WithMockUser(username = "c06", roles = "ELEITOR")
        @DisplayName("revisar mostra \"Confira seu voto\" com a opção; voltar mantém a marcação")
        void revisar() throws Exception {
            Votacao votacao = aberta(votacao("c06"));

            mvc.perform(post(url(votacao, "/revisar")).param("opcao", VERDE).with(csrf()))
                    .andExpect(view().name("eleitor/revisar"))
                    .andExpect(content().string(allOf(containsString("Confira seu voto"), containsString(VERDE),
                            containsString("Confirmar voto"), containsString("Voltar e mudar"))));
            mvc.perform(get(url(votacao, "")).param("opcao", VERDE))
                    .andExpect(content().string(containsString("value=\"" + VERDE + "\" checked")));
        }

        @Test
        @WithMockUser(username = "c07", roles = "ELEITOR")
        @DisplayName("revisar numa votação fechada volta para a cédula")
        void revisarFechada() throws Exception {
            Votacao votacao = votacoes.salvar(votacao("c07").build());

            mvc.perform(post(url(votacao, "/revisar")).param("opcao", AZUL).with(csrf()))
                    .andExpect(redirectedUrl(url(votacao, "")));
        }
    }

    @Nested
    @DisplayName("Voto e comprovante")
    class Voto {

        @Test
        @WithMockUser(username = "v01", roles = "ELEITOR")
        @DisplayName("registra, mostra o comprovante uma vez (sem a opção) e recusa o segundo voto")
        void votarUmaVez() throws Exception {
            Votacao votacao = aberta(votacao("v01"));

            MvcResult voto = mvc.perform(post(url(votacao, "/voto")).param("opcao", AZUL).with(csrf()))
                    .andExpect(redirectedUrl(url(votacao, "/comprovante")))
                    .andReturn();
            ComprovanteVoto comprovante = (ComprovanteVoto) voto.getFlashMap().get("comprovante");
            assertTrue(votacao.comprovanteValido(comprovante.codigo()));

            mvc.perform(get(url(votacao, "/comprovante")).flashAttrs(voto.getFlashMap()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Voto registrado"), containsString(comprovante.codigo()),
                            containsString("class=\"codigo\""), containsString("Copiar código"),
                            containsString("/verificar?votacao=" + votacao.id()
                                    + "&amp;codigo=" + comprovante.codigo()),
                            not(containsString(AZUL)))));

            mvc.perform(get(url(votacao, "/comprovante")))
                    .andExpect(redirectedUrl("/eleitor"))
                    .andExpect(flash().attribute("alerta", Alerta.info("Seu voto já foi registrado.")));

            mvc.perform(post(url(votacao, "/voto")).param("opcao", VERDE).with(csrf()))
                    .andExpect(redirectedUrl(url(votacao, "")))
                    .andExpect(flash().attribute("alerta", Alerta.erro("Você já votou nesta votação.")));
            assertEquals(1, votacao.totalVotantes());
        }

        @Test
        @WithMockUser(username = "v02", roles = "ELEITOR")
        @DisplayName("sigilo: depois do voto, nem o painel nem a cédula mostram a opção escolhida")
        void sigiloDepoisDoVoto() throws Exception {
            Votacao votacao = aberta(votacao("v02"));
            mvc.perform(post(url(votacao, "/voto")).param("opcao", VERDE).with(csrf()));

            mvc.perform(get(url(votacao, "")))
                    .andExpect(view().name("eleitor/indisponivel"))
                    .andExpect(content().string(allOf(containsString("Você já votou nesta votação"),
                            containsString("Verificar comprovante"), not(containsString(VERDE)))));
            mvc.perform(get("/eleitor"))
                    .andExpect(content().string(allOf(containsString("Já votei"), not(containsString(VERDE)))));
        }

        @Test
        @WithMockUser(username = "v03", roles = "ELEITOR")
        @DisplayName("comprovante sem voto volta para a cédula")
        void comprovanteSemVoto() throws Exception {
            Votacao votacao = aberta(votacao("v03"));

            mvc.perform(get(url(votacao, "/comprovante"))).andExpect(redirectedUrl(url(votacao, "")));
        }

        @Test
        @WithMockUser(username = "v04", roles = "ELEITOR")
        @DisplayName("votação em rascunho recusa o voto e mostra que não está recebendo votos")
        void rascunho() throws Exception {
            Votacao votacao = votacoes.salvar(votacao("v04").build());

            mvc.perform(post(url(votacao, "/voto")).param("opcao", AZUL).with(csrf()))
                    .andExpect(redirectedUrl(url(votacao, "")))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("Esta ação não é permitida com a votação em rascunho.")));
            mvc.perform(get(url(votacao, "")))
                    .andExpect(view().name("eleitor/indisponivel"))
                    .andExpect(content().string(containsString("Esta votação não está recebendo votos agora")));
        }

        @Test
        @WithMockUser(username = "v05", roles = "ELEITOR")
        @DisplayName("fora do período recusa o voto e mostra as datas")
        void foraDoPeriodo() throws Exception {
            PeriodoVotacao futuro = new PeriodoVotacao(relogio.instant().plus(Duration.ofDays(1)),
                    relogio.instant().plus(Duration.ofDays(2)));
            Votacao votacao = aberta(votacao("v05").periodo(futuro));

            mvc.perform(post(url(votacao, "/voto")).param("opcao", AZUL).with(csrf()))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("A votação não está recebendo votos neste horário.")));
            mvc.perform(get(url(votacao, "")))
                    .andExpect(content().string(allOf(containsString("Abre em"), containsString("Encerra em"))));
        }

        @Test
        @WithMockUser(username = "v06", roles = "ELEITOR")
        @DisplayName("opção inexistente ou ausente no POST do voto é recusada com mensagem amigável")
        void opcaoInvalida() throws Exception {
            Votacao votacao = aberta(votacao("v06"));

            mvc.perform(post(url(votacao, "/voto")).param("opcao", "Chapa Fantasma").with(csrf()))
                    .andExpect(flash().attribute("alerta", OPCAO_INEXISTENTE));
            mvc.perform(post(url(votacao, "/voto")).with(csrf()))
                    .andExpect(flash().attribute("alerta", OPCAO_INEXISTENTE));
            assertEquals(0, votacao.totalVotantes());
        }
    }

    @Nested
    @DisplayName("Acesso")
    class Acesso {

        @Test
        @WithMockUser(username = "organizador", roles = "ORGANIZADOR")
        @DisplayName("organizador recebe 403 em /eleitor/**")
        void organizador() throws Exception {
            mvc.perform(get("/eleitor")).andExpect(status().isForbidden());
            mvc.perform(get("/eleitor/votacoes/qualquer")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("sem login, /eleitor redireciona para o login")
        void anonimo() throws Exception {
            mvc.perform(get("/eleitor")).andExpect(status().is3xxRedirection());
        }
    }
}
