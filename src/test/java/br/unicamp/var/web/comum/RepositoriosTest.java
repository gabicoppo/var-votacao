package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.processo.Votacao;
import br.unicamp.var.top3.CategoriaEleitor;
import br.unicamp.var.top3.PremiacaoTop3;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Repositórios em memória: ordem (mais recentes primeiro), busca, id desconhecido e sobrescrita.
 */
class RepositoriosTest {

    private static final Instant INICIO = Instant.parse("2026-11-24T12:00:00Z");

    /** Relógio que o teste adianta. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = INICIO;

        void avancarMinutos(long minutos) {
            agora = agora.plusSeconds(60 * minutos);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();

    private Votacao votacao(String pergunta) {
        return Votacao.nova()
                .pergunta(pergunta)
                .opcoes(List.of("Sim", "Não"))
                .regra(new MaioriaSimples())
                .eleitorado(Eleitorado.de(List.of(new Eleitor("e01", "Ana Ribeiro"))))
                .relogio(relogio)
                .build();
    }

    private static PremiacaoTop3 premiacao(String titulo) {
        return new PremiacaoTop3(titulo, "", List.of("Atleta A", "Atleta B", "Atleta C"),
                List.of(new CategoriaEleitor("Torcedores", 1.0)), Map.of("e01", "Torcedores"));
    }

    @Nested
    @DisplayName("RepositorioVotacoes")
    class Votacoes {

        private final RepositorioVotacoes repositorio = new RepositorioVotacoesEmMemoria();

        @Test
        @DisplayName("lista das mais recentes para as mais antigas")
        void listaMaisRecentesPrimeiro() {
            Votacao antiga = repositorio.salvar(votacao("Antiga"));
            relogio.avancarMinutos(5);
            Votacao nova = repositorio.salvar(votacao("Nova"));
            relogio.avancarMinutos(5);
            Votacao maisNova = repositorio.salvar(votacao("Mais nova"));

            assertEquals(List.of(maisNova, nova, antiga), repositorio.listar());
        }

        @Test
        @DisplayName("busca pelo id; id nulo ou desconhecido dá vazio")
        void busca() {
            Votacao votacao = repositorio.salvar(votacao("Pergunta"));

            assertSame(votacao, repositorio.buscar(votacao.id()).orElseThrow());
            assertEquals(Optional.empty(), repositorio.buscar("desconhecido"));
            assertEquals(Optional.empty(), repositorio.buscar(null));
        }

        @Test
        @DisplayName("salvar de novo a mesma votação sobrescreve, sem duplicar")
        void sobrescreve() {
            Votacao votacao = votacao("Pergunta");
            repositorio.salvar(votacao);
            repositorio.salvar(votacao);

            assertEquals(1, repositorio.listar().size());
        }

        @Test
        @DisplayName("votação nula é recusada; lista vazia no início e imutável")
        void validacoes() {
            assertThrows(IllegalArgumentException.class, () -> repositorio.salvar(null));
            assertTrue(repositorio.listar().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> repositorio.listar().add(votacao("x")));
        }
    }

    @Nested
    @DisplayName("RepositorioPremiacoes")
    class Premiacoes {

        private final RepositorioPremiacoes repositorio = new RepositorioPremiacoesEmMemoria(relogio);

        @Test
        @DisplayName("salvar gera id (UUID) e horário de cadastro pelo Clock")
        void salvarGeraIdEHorario() {
            PremiacaoTop3 premiacao = premiacao("Craque");
            PremiacaoCadastrada cadastro = repositorio.salvar(premiacao);

            assertSame(premiacao, cadastro.premiacao());
            assertEquals(INICIO, cadastro.criadaEm());
            assertEquals(36, cadastro.id().length());
            assertNotEquals(cadastro.id(), repositorio.salvar(premiacao).id());
        }

        @Test
        @DisplayName("lista das mais recentes para as mais antigas")
        void listaMaisRecentesPrimeiro() {
            PremiacaoCadastrada antiga = repositorio.salvar(premiacao("Antiga"));
            relogio.avancarMinutos(1);
            PremiacaoCadastrada nova = repositorio.salvar(premiacao("Nova"));

            assertEquals(List.of(nova, antiga), repositorio.listar());
        }

        @Test
        @DisplayName("busca pelo id; id nulo ou desconhecido dá vazio")
        void busca() {
            PremiacaoCadastrada cadastro = repositorio.salvar(premiacao("Craque"));

            assertEquals(Optional.of(cadastro), repositorio.buscar(cadastro.id()));
            assertEquals(Optional.empty(), repositorio.buscar("desconhecido"));
            assertEquals(Optional.empty(), repositorio.buscar(null));
        }

        @Test
        @DisplayName("premiação nula é recusada")
        void premiacaoNula() {
            assertThrows(IllegalArgumentException.class, () -> repositorio.salvar(null));
        }
    }
}
