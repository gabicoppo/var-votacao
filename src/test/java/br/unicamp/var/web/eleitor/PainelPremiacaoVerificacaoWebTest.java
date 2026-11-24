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
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import br.unicamp.var.urna.Comprovante;
import br.unicamp.var.web.comum.Alerta;
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
 * Painel do eleitor, cédula Top 3 (história #23) e verificação pública de comprovante (#28).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PainelPremiacaoVerificacaoWebTest {

    private static final List<String> JOGADORES = List.of("Atleta Alfa", "Atleta Beta", "Atleta Gama");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    @Autowired
    private Clock relogio;

    private Votacao votacao(String pergunta, String... eleitores) {
        return votacoes.salvar(Votacao.nova()
                .pergunta(pergunta)
                .opcoes(List.of("Opção Um", "Opção Dois"))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(Arrays.stream(eleitores)
                        .map(id -> new Eleitor(id, "Eleitor " + id)).toList()))
                .relogio(relogio)
                .build());
    }

    private PremiacaoCadastrada premiacao(String titulo, String jornalista, String torcedor) {
        PremiacaoTop3 premiacao = new PremiacaoTop3(titulo, "Melhor jogador", JOGADORES,
                List.of(new CategoriaEleitor("Jornalistas", 2.0), new CategoriaEleitor("Torcedores", 1.0)),
                Map.of(jornalista, "Jornalistas", torcedor, "Torcedores"));
        return premiacoes.salvar(premiacao);
    }

    @Nested
    @DisplayName("Painel")
    class Painel {

        @Test
        @WithMockUser(username = "p01", roles = "ELEITOR")
        @DisplayName("mostra saudação, resumo e cada seção, incluindo a premiação Top 3")
        void secoes() throws Exception {
            Votacao aberta = votacao("Votação aberta do painel", "p01");
            aberta.abrir();
            Votacao apurada = votacao("Votação apurada do painel", "p01");
            apurada.abrir();
            apurada.encerrar();
            apurada.apurar();
            votacao("Votação de outros", "outro");
            PremiacaoCadastrada top3 = premiacao("Craque do painel", "x01", "p01");
            top3.premiacao().abrir();

            mvc.perform(get("/eleitor"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Olá, p01!"),
                            containsString("<strong>2 votações</strong>"),
                            containsString("Para votar agora"), containsString("card-destaque"),
                            containsString("Votação aberta do painel"),
                            containsString("Craque do painel"), containsString(">Top 3<"),
                            containsString("/eleitor/premiacoes/" + top3.id()),
                            containsString("Resultados"), containsString("/votacoes/" + apurada.id() + "/resultado"),
                            not(containsString("Votação de outros")))));
        }

        @Test
        @WithMockUser(username = "p02", roles = "ELEITOR")
        @DisplayName("sem nada para o eleitor, mostra o estado vazio")
        void vazio() throws Exception {
            mvc.perform(get("/eleitor"))
                    .andExpect(content().string(allOf(
                            containsString("Nenhuma votação para você agora"),
                            containsString("Nenhuma votação esperando seu voto agora."))));
        }

        @Test
        @WithMockUser(username = "p03", roles = "ELEITOR")
        @DisplayName("com uma votação, o resumo fica no singular")
        void singular() throws Exception {
            votacao("Só uma", "p03").abrir();

            mvc.perform(get("/eleitor")).andExpect(content().string(containsString("<strong>1 votação</strong>")));
        }

        @Test
        @DisplayName("primeiro nome para a saudação")
        void primeiroNome() {
            assertEquals("Ana", PainelEleitorController.primeiroNome("  Ana Ribeiro "));
        }
    }

    @Nested
    @DisplayName("Cédula Top 3")
    class Top3 {

        @Test
        @WithMockUser(username = "t01", roles = "ELEITOR")
        @DisplayName("mostra a categoria e o peso, a regra e os três selects")
        void cedula() throws Exception {
            PremiacaoCadastrada cadastro = premiacao("Craque T1", "t01", "x02");
            cadastro.premiacao().abrir();

            mvc.perform(get("/eleitor/premiacoes/" + cadastro.id()))
                    .andExpect(view().name("eleitor/premiacao-cedula"))
                    .andExpect(content().string(allOf(
                            containsString("Você vota como <strong>Jornalistas</strong>"),
                            containsString("(peso <strong>2</strong>)"),
                            containsString("1º = 5 pontos, 2º = 3, 3º = 1"),
                            containsString("name=\"primeiro\""), containsString("name=\"terceiro\""))));
        }

        @Test
        @WithMockUser(username = "t02", roles = "ELEITOR")
        @DisplayName("candidatos repetidos, vazios ou fora da lista são recusados na revisão")
        void validacao() throws Exception {
            PremiacaoCadastrada cadastro = premiacao("Craque T2", "x03", "t02");
            cadastro.premiacao().abrir();
            String url = "/eleitor/premiacoes/" + cadastro.id() + "/revisar";

            mvc.perform(post(url).with(csrf()).param("primeiro", "Atleta Alfa").param("segundo", "Atleta Alfa")
                            .param("terceiro", "Atleta Gama"))
                    .andExpect(view().name("eleitor/premiacao-cedula"))
                    .andExpect(content().string(containsString("Escolha três candidatos diferentes.")));
            mvc.perform(post(url).with(csrf()).param("primeiro", "Atleta Alfa"))
                    .andExpect(content().string(containsString("Escolha o 1º, o 2º e o 3º lugar.")));
            mvc.perform(post(url).with(csrf()).param("primeiro", "Atleta Alfa").param("segundo", "Atleta Beta")
                            .param("terceiro", "Atleta Ômega"))
                    .andExpect(content().string(containsString("Escolha apenas candidatos da lista.")));
        }

        @Test
        @WithMockUser(username = "t03", roles = "ELEITOR")
        @DisplayName("revisa, registra a cédula válida, mostra o sucesso uma vez e recusa o duplo voto")
        void registrar() throws Exception {
            PremiacaoCadastrada cadastro = premiacao("Craque T3", "x04", "t03");
            cadastro.premiacao().abrir();
            String base = "/eleitor/premiacoes/" + cadastro.id();

            mvc.perform(post(base + "/revisar").with(csrf()).param("primeiro", "Atleta Gama")
                            .param("segundo", "Atleta Alfa").param("terceiro", "Atleta Beta"))
                    .andExpect(view().name("eleitor/premiacao-revisar"))
                    .andExpect(content().string(allOf(containsString("Confira sua cédula"),
                            containsString("Confirmar cédula"))));
            MvcResult voto = mvc.perform(post(base + "/voto").with(csrf()).param("primeiro", "Atleta Gama")
                            .param("segundo", "Atleta Alfa").param("terceiro", "Atleta Beta"))
                    .andExpect(redirectedUrl(base + "/sucesso"))
                    .andReturn();
            assertTrue(cadastro.premiacao().jaVotou("t03"));

            mvc.perform(get(base + "/sucesso").flashAttrs(voto.getFlashMap()))
                    .andExpect(content().string(containsString("Cédula registrada")));
            mvc.perform(get(base + "/sucesso"))
                    .andExpect(redirectedUrl("/eleitor"))
                    .andExpect(flash().attribute("alerta", Alerta.info("Sua cédula já foi registrada.")));
            mvc.perform(post(base + "/voto").with(csrf()).param("primeiro", "Atleta Alfa")
                            .param("segundo", "Atleta Beta").param("terceiro", "Atleta Gama"))
                    .andExpect(redirectedUrl(base))
                    .andExpect(flash().attribute("alerta", Alerta.erro("Você já votou nesta votação.")));
            mvc.perform(get(base)).andExpect(content().string(containsString("Você já votou nesta premiação")));
            assertEquals(1, cadastro.premiacao().totalCedulas());
        }

        @Test
        @WithMockUser(username = "t04", roles = "ELEITOR")
        @DisplayName("premiação em rascunho não recebe cédulas; sucesso sem voto volta à cédula")
        void fechada() throws Exception {
            PremiacaoCadastrada cadastro = premiacao("Craque T4", "x05", "t04");
            String base = "/eleitor/premiacoes/" + cadastro.id();

            mvc.perform(get(base))
                    .andExpect(content().string(
                            containsString("Esta premiação não está recebendo cédulas agora")));
            mvc.perform(post(base + "/revisar").with(csrf())).andExpect(redirectedUrl(base));
            mvc.perform(post(base + "/voto").with(csrf()).param("primeiro", "Atleta Alfa")
                            .param("segundo", "Atleta Beta").param("terceiro", "Atleta Gama"))
                    .andExpect(flash().attribute("alerta",
                            Alerta.erro("Esta ação não é permitida no estado atual da premiação.")));
            mvc.perform(get(base + "/sucesso")).andExpect(redirectedUrl(base));
        }

        @Test
        @WithMockUser(username = "fora", roles = "ELEITOR")
        @DisplayName("eleitor sem categoria recebe 403; premiação inexistente dá 404")
        void naoApto() throws Exception {
            PremiacaoCadastrada cadastro = premiacao("Craque T5", "x06", "x07");
            cadastro.premiacao().abrir();

            mvc.perform(get("/eleitor/premiacoes/" + cadastro.id())).andExpect(status().isForbidden());
            mvc.perform(get("/eleitor/premiacoes/nao-existe")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Verificar comprovante (público)")
    class Verificar {

        private Votacao votacaoComVoto() {
            Votacao votacao = votacao("Votação verificável", "w01");
            votacao.abrir();
            return votacao;
        }

        @Test
        @DisplayName("sem login, mostra o formulário com as votações que já receberam votos")
        void formulario() throws Exception {
            Votacao votacao = votacaoComVoto();
            Votacao rascunho = votacao("Rascunho escondido", "w02");

            mvc.perform(get("/verificar"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(containsString("Código do comprovante"),
                            containsString(votacao.id()), not(containsString(rascunho.id())))));
        }

        @Test
        @DisplayName("código válido confirma a contagem sem dizer qual foi o voto (aceita minúsculas)")
        void valido() throws Exception {
            Votacao votacao = votacaoComVoto();
            Comprovante comprovante = votacao.registrarVoto("w01", "Opção Dois");

            mvc.perform(get("/verificar").param("votacao", votacao.id())
                            .param("codigo", " " + comprovante.codigo().toLowerCase() + " "))
                    .andExpect(content().string(allOf(
                            containsString("Este comprovante foi emitido por esta votação"),
                            containsString("seu voto está na contagem."),
                            not(containsString("Opção Dois")))));
        }

        @Test
        @DisplayName("código inválido e votação inexistente têm mensagens próprias")
        void invalido() throws Exception {
            Votacao votacao = votacaoComVoto();

            mvc.perform(get("/verificar").param("votacao", votacao.id()).param("codigo", "ZZZZZZZZ"))
                    .andExpect(content().string(containsString("Código não encontrado nesta votação")));
            mvc.perform(get("/verificar").param("votacao", "nao-existe").param("codigo", "ZZZZZZZZ"))
                    .andExpect(content().string(containsString("Votação não encontrada")));
        }
    }
}
