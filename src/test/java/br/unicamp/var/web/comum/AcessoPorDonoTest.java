package br.unicamp.var.web.comum;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Organizadores isolados: cada um vê e gerencia só as votações e premiações que criou.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AcessoPorDonoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositorioVotacoes votacoes;

    @Autowired
    private RepositorioPremiacoes premiacoes;

    @Autowired
    private Clock relogio;

    private Votacao votacao(String pergunta, String criadaPor) {
        Votacao.Builder builder = Votacao.nova()
                .pergunta(pergunta)
                .opcoes(List.of("Sim", "Não"))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(List.of(new Eleitor("d01", "Eleitor d01"))))
                .relogio(relogio);
        Votacao votacao = criadaPor == null ? builder.build() : builder.criadaPor(criadaPor).build();
        return votacoes.salvar(votacao);
    }

    private String premiacao(String titulo, String dono) {
        PremiacaoTop3 p = new PremiacaoTop3(titulo, "", List.of("A", "B", "C"),
                List.of(new CategoriaEleitor("Todos", 1.0)), Map.of("d01", "Todos"));
        return premiacoes.salvar(p, dono).id();
    }

    @Nested
    @DisplayName("Votações")
    class Votacoes {

        @Test
        @WithMockUser(username = "bia", roles = "ORGANIZADOR")
        @DisplayName("a votação de outro organizador dá 404 na gestão, nos acessos, no resultado e no telão")
        void deOutro() throws Exception {
            Votacao daAna = votacao("Votação da Ana", "ana");

            mvc.perform(get("/organizador/votacoes/" + daAna.id())).andExpect(status().isNotFound());
            mvc.perform(get("/organizador/votacoes/" + daAna.id() + "/acessos")).andExpect(status().isNotFound());
            mvc.perform(get("/votacoes/" + daAna.id() + "/resultado")).andExpect(status().isNotFound());
            mvc.perform(get("/votacoes/" + daAna.id() + "/telao")).andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "ana", roles = "ORGANIZADOR")
        @DisplayName("a dona abre a gestão; o painel mostra só as dela e as sem dono")
        void daDona() throws Exception {
            Votacao daAna = votacao("Votação exclusiva da Ana", "ana");
            votacao("Votação exclusiva do Caio", "caio");
            votacao("Votação do sistema", null);

            mvc.perform(get("/organizador/votacoes/" + daAna.id())).andExpect(status().isOk());
            mvc.perform(get("/organizador"))
                    .andExpect(content().string(allOf(
                            containsString("Votação exclusiva da Ana"), containsString("Votação do sistema"),
                            not(containsString("Votação exclusiva do Caio")))));
        }

        @Test
        @WithMockUser(username = "bia", roles = "ORGANIZADOR")
        @DisplayName("votação sem dono (criada pelo sistema) vale para todos os organizadores")
        void semDono() throws Exception {
            Votacao doSistema = votacao("Votação aberta a todos", null);

            mvc.perform(get("/organizador/votacoes/" + doSistema.id())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "d01", roles = "ELEITOR")
        @DisplayName("eleitores não passam pela regra de dono: o apto abre a cédula")
        void eleitor() throws Exception {
            Votacao daAna = votacao("Cédula da Ana", "ana");
            daAna.abrir();

            mvc.perform(get("/eleitor/votacoes/" + daAna.id())).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "bia", roles = "ORGANIZADOR")
        @DisplayName("ids inexistentes e as rotas sem id continuam com o comportamento normal")
        void semRecurso() throws Exception {
            mvc.perform(get("/organizador/votacoes/nova")).andExpect(status().isOk());
            mvc.perform(get("/organizador/votacoes/nao-existe")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Premiações")
    class Premiacoes {

        @Test
        @WithMockUser(username = "bia", roles = "ORGANIZADOR")
        @DisplayName("a premiação de outro organizador dá 404 na gestão e no ranking; a sem dono abre")
        void deOutro() throws Exception {
            String daAna = premiacao("Premiação da Ana", "ana");
            String semDono = premiacao("Premiação aberta", null);

            mvc.perform(get("/organizador/premiacoes/" + daAna)).andExpect(status().isNotFound());
            mvc.perform(get("/premiacoes/" + daAna + "/ranking")).andExpect(status().isNotFound());
            mvc.perform(get("/organizador/premiacoes/" + semDono)).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "ana", roles = "ORGANIZADOR")
        @DisplayName("a dona abre a gestão, e o painel não mostra as premiações dos outros")
        void daDona() throws Exception {
            String daAna = premiacao("Premiação exclusiva da Ana", "ana");
            premiacao("Premiação exclusiva do Caio", "caio");

            mvc.perform(get("/organizador/premiacoes/" + daAna)).andExpect(status().isOk());
            mvc.perform(get("/organizador"))
                    .andExpect(content().string(allOf(containsString("Premiação exclusiva da Ana"),
                            not(containsString("Premiação exclusiva do Caio")))));
        }

        @Test
        @DisplayName("o repositório guarda o dono; premiação sem dono e id nulo dão vazio")
        void repositorio() {
            String daAna = premiacao("Premiação com dono", "ana");
            String semDono = premiacao("Premiação sem dono", " ");

            assertEquals(Optional.of("ana"), premiacoes.donoDe(daAna));
            assertEquals(Optional.empty(), premiacoes.donoDe(semDono));
            assertEquals(Optional.empty(), premiacoes.donoDe(null));
        }
    }
}
