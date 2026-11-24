package br.unicamp.var.web.eleitor;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.urna.Comprovante;
import br.unicamp.var.web.comum.PremiacaoCadastrada;
import br.unicamp.var.web.comum.RepositorioPremiacoes;
import br.unicamp.var.web.comum.RepositorioVotacoes;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * Acabamento da área do eleitor (Fase 3; histórias #23, #26 e #28; RNF-01): etiqueta de peso nas
 * premiações, estado "em dia", cédula Top 3 por toque com os selects preservados, comprovante para
 * salvar e compartilhar, verificação com resultado direto e página "não apto".
 */
@SpringBootTest
@AutoConfigureMockMvc
class EleitorPolimentoWebTest {

    private static final String SIM = "Opção Sim Polimento";
    private static final String NAO = "Opção Não Polimento";
    private static final List<String> JOGADORES = List.of("Atleta Alfa", "Atleta Beta", "Atleta Gama", "Atleta Delta");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    @Autowired
    private Clock relogio;

    private Votacao aberta(String pergunta, String... eleitores) {
        Votacao votacao = Votacao.nova()
                .pergunta(pergunta)
                .opcoes(List.of(SIM, NAO))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(Arrays.stream(eleitores)
                        .map(id -> new Eleitor(id, "Eleitor " + id)).toList()))
                .relogio(relogio)
                .build();
        votacao.abrir();
        return votacoes.salvar(votacao);
    }

    private PremiacaoCadastrada premiacaoAberta(String titulo, String jornalista, String torcedor) {
        PremiacaoTop3 premiacao = new PremiacaoTop3(titulo, "Melhor jogador", JOGADORES,
                List.of(new CategoriaEleitor("Jornalistas", 2.0), new CategoriaEleitor("Torcedores", 1.0)),
                Map.of(jornalista, "Jornalistas", torcedor, "Torcedores"));
        premiacao.abrir();
        return premiacoes.salvar(premiacao);
    }

    @Nested
    @DisplayName("Painel")
    class Painel {

        @Test
        @WithMockUser(username = "pp01", roles = "ELEITOR")
        @DisplayName("card da premiação mostra o peso do grupo e não \"Voto secreto\"")
        void premiacaoMostraPeso() throws Exception {
            premiacaoAberta("Craque do polimento", "pp01", "pp99");

            mvc.perform(get("/eleitor"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Craque do polimento"), containsString("Top 3 · peso 2"),
                            not(containsString("Voto secreto")))));
        }

        @Test
        @WithMockUser(username = "pp02", roles = "ELEITOR")
        @DisplayName("sem nada para votar, mas com votações já votadas, mostra \"Você está em dia!\"")
        void emDia() throws Exception {
            Votacao votacao = aberta("Votação já votada do polimento", "pp02");
            votacao.registrarVoto("pp02", SIM);

            mvc.perform(get("/eleitor"))
                    .andExpect(content().string(allOf(
                            containsString("Você está em dia!"),
                            containsString("Nenhuma votação esperando seu voto."),
                            containsString("Já votei"))));
        }

        @Test
        @WithMockUser(username = "pp03", roles = "ELEITOR")
        @DisplayName("com algo para votar, não mostra o estado \"em dia\"")
        void naoEmDia() throws Exception {
            aberta("Votação pendente do polimento", "pp03");

            mvc.perform(get("/eleitor")).andExpect(content().string(not(containsString("Você está em dia!"))));
        }
    }

    @Nested
    @DisplayName("Cédula Top 3 por toque")
    class CedulaTop3 {

        @Test
        @WithMockUser(username = "pt01", roles = "ELEITOR")
        @DisplayName("traz os cartões de candidatos (escondidos sem JS) e mantém os 3 selects do POST")
        void cartoesESelects() throws Exception {
            PremiacaoCadastrada cadastro = premiacaoAberta("Craque toque", "pt01", "pt99");

            mvc.perform(get("/eleitor/premiacoes/" + cadastro.id()))
                    .andExpect(view().name("eleitor/premiacao-cedula"))
                    .andExpect(content().string(allOf(
                            containsString("data-ranking hidden"),
                            containsString("data-candidato=\"Atleta Delta\""),
                            containsString("aria-pressed=\"false\""),
                            containsString("aria-live=\"polite\""),
                            containsString(">Limpar<"),
                            containsString("name=\"primeiro\""), containsString("name=\"segundo\""),
                            containsString("name=\"terceiro\""),
                            containsString("/js/eleitor.js"))));
        }

        @Test
        @WithMockUser(username = "pt02", roles = "ELEITOR")
        @DisplayName("o POST com os 3 campos continua revisando e registrando a cédula")
        void postDosTresCampos() throws Exception {
            PremiacaoCadastrada cadastro = premiacaoAberta("Craque POST", "pt02", "pt98");
            String base = "/eleitor/premiacoes/" + cadastro.id();

            mvc.perform(post(base + "/revisar").with(csrf()).param("primeiro", "Atleta Delta")
                            .param("segundo", "Atleta Alfa").param("terceiro", "Atleta Gama"))
                    .andExpect(view().name("eleitor/premiacao-revisar"))
                    .andExpect(content().string(allOf(containsString("Atleta Delta"),
                            containsString("data-registrando"), containsString("🥇"))));
            mvc.perform(post(base + "/voto").with(csrf()).param("primeiro", "Atleta Delta")
                            .param("segundo", "Atleta Alfa").param("terceiro", "Atleta Gama"))
                    .andExpect(redirectedUrl(base + "/sucesso"));
        }
    }

    @Nested
    @DisplayName("Voto e comprovante")
    class Comprovantes {

        @Test
        @WithMockUser(username = "pc01", roles = "ELEITOR")
        @DisplayName("cédula e revisão têm os ganchos do JS (habilitar e \"Registrando…\")")
        void cedulaERevisao() throws Exception {
            Votacao votacao = aberta("Votação da cédula do polimento", "pc01");
            String base = "/eleitor/votacoes/" + votacao.id();

            mvc.perform(get(base))
                    .andExpect(content().string(allOf(containsString("data-cedula"), containsString("data-revisar"),
                            containsString("Toque numa opção para continuar."))));
            mvc.perform(post(base + "/revisar").with(csrf()).param("opcao", NAO))
                    .andExpect(content().string(allOf(containsString("escolha-opcao"), containsString(NAO),
                            containsString("data-registrando"), containsString("Confirmar voto"))));
        }

        @Test
        @WithMockUser(username = "pc02", roles = "ELEITOR")
        @DisplayName("comprovante tem salvar, compartilhar, o link completo e o aviso, sem a opção (RNF-01)")
        void comprovante() throws Exception {
            Votacao votacao = aberta("Votação do comprovante do polimento", "pc02");
            String base = "/eleitor/votacoes/" + votacao.id();

            MvcResult voto = mvc.perform(post(base + "/voto").with(csrf()).param("opcao", SIM)).andReturn();
            ComprovanteVoto comprovante = (ComprovanteVoto) voto.getFlashMap().get("comprovante");

            mvc.perform(get(base + "/comprovante").flashAttrs(voto.getFlashMap()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Salvar comprovante"), containsString("Compartilhar"),
                            containsString("Copiar código"),
                            containsString("Guarde este código. Ele prova que seu voto foi contado"),
                            containsString("não revela em quem você votou"),
                            containsString("http://localhost/verificar?votacao=" + votacao.id()
                                    + "&amp;codigo=" + comprovante.codigo()),
                            not(containsString(SIM)), not(containsString(NAO)))));
        }
    }

    @Nested
    @DisplayName("Verificar comprovante")
    class Verificar {

        @Test
        @DisplayName("pelo link do comprovante, o resultado aparece direto: \"Voto contado\"")
        void valido() throws Exception {
            Votacao votacao = aberta("Votação verificada do polimento", "pv01");
            Comprovante comprovante = votacao.registrarVoto("pv01", NAO);

            mvc.perform(get("/verificar").param("votacao", votacao.id()).param("codigo", comprovante.codigo()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("veredito-ok"), containsString("Voto contado"),
                            containsString("não mostra em quem você votou"),
                            containsString("Verificar outro código"),
                            not(containsString(NAO)))));
        }

        @Test
        @DisplayName("código desconhecido mostra \"✗ Código não encontrado nesta votação\" com a explicação")
        void invalido() throws Exception {
            Votacao votacao = aberta("Votação não encontrada do polimento", "pv02");

            mvc.perform(get("/verificar").param("votacao", votacao.id()).param("codigo", "ZZZZ9999ZZ"))
                    .andExpect(content().string(allOf(
                            containsString("veredito-erro"), containsString("✗"),
                            containsString("Código não encontrado nesta votação"),
                            containsString("não mostra nem altera nenhum voto"))));
        }

        @Test
        @DisplayName("sem parâmetros, mostra só o formulário")
        void semParametros() throws Exception {
            mvc.perform(get("/verificar"))
                    .andExpect(content().string(allOf(containsString("Informe o comprovante"),
                            not(containsString("veredito")))));
        }
    }

    @Nested
    @DisplayName("Não apto")
    class NaoApto {

        @Test
        @WithMockUser(username = "pn01", roles = "ELEITOR")
        @DisplayName("mostra o título da votação (não o do layout) e preenche o cabeçalho")
        void tituloECabecalho() throws Exception {
            Votacao votacao = aberta("Votação restrita do polimento", "outra-pessoa");

            mvc.perform(get("/eleitor/votacoes/" + votacao.id()))
                    .andExpect(status().isForbidden())
                    .andExpect(model().attribute("nomeUsuario", "pn01"))
                    .andExpect(model().attribute("caminhoAtual", "/eleitor/votacoes/" + votacao.id()))
                    .andExpect(content().string(allOf(
                            containsString("Votação restrita do polimento"),
                            not(containsString("&lt;title&gt;")))));
        }
    }
}
