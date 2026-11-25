package br.unicamp.var.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unicamp.var.apuracao.MaioriaAbsoluta;
import br.unicamp.var.apuracao.MaioriaSimples;
import br.unicamp.var.apuracao.RegraApuracao;
import br.unicamp.var.auditoria.EventoAuditoria;
import br.unicamp.var.auditoria.TipoEvento;
import br.unicamp.var.eleitorado.Eleitor;
import br.unicamp.var.eleitorado.Eleitorado;
import br.unicamp.var.urna.Sigilo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Auditoria da {@link Votacao} (RNF-07): cada transição bem-sucedida gera um evento com data/hora
 * do relógio da votação e autor; operação proibida não gera evento; votos nunca aparecem (RNF-01).
 */
final class VotacaoAuditoriaTest {

    private static final Instant CRIACAO = Instant.parse("2027-02-20T12:00:00Z");
    private static final String ORGANIZADOR = "organizador";
    private static final String SISTEMA = Votacao.AUTOR_SISTEMA;

    /** Relógio de teste que o teste adianta quando quiser. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora = CRIACAO;

        void avancarMinutos(long minutos) {
            agora = agora.plusSeconds(minutos * 60);
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

    private static Eleitorado eleitorado(int total) {
        return Eleitorado.de(IntStream.rangeClosed(1, total)
                .mapToObj(i -> new Eleitor("e" + i, "Eleitor " + i))
                .toList());
    }

    private Votacao.Builder configuracao() {
        return Votacao.nova()
                .pergunta("Quem deve ser o presidente?")
                .opcoes(List.of("Chapa A", "Chapa B"))
                .regra(new MaioriaSimples())
                .eleitorado(eleitorado(5))
                .relogio(relogio);
    }

    private static List<TipoEvento> tipos(Votacao votacao) {
        return votacao.auditoria().stream().map(EventoAuditoria::tipo).toList();
    }

    private static EventoAuditoria ultimo(Votacao votacao) {
        List<EventoAuditoria> eventos = votacao.auditoria();
        return eventos.get(eventos.size() - 1);
    }

    @Nested
    @DisplayName("Criação")
    final class Criacao {

        @Test
        @DisplayName("build() registra CRIADA com o autor padrão \"sistema\" e o horário do relógio")
        void criacaoRegistraEventoComAutorPadrao() {
            Votacao votacao = configuracao().build();

            assertEquals(List.of(new EventoAuditoria(CRIACAO, SISTEMA, TipoEvento.CRIADA, "")), votacao.auditoria());
        }

        @Test
        @DisplayName("criadaPor define o autor do evento CRIADA")
        void criadaPorDefineAutor() {
            Votacao votacao = configuracao().criadaPor(ORGANIZADOR).build();

            assertEquals(ORGANIZADOR, votacao.auditoria().get(0).autor());
        }

        @Test
        @DisplayName("criadaPor com autor nulo ou em branco é recusado no build()")
        void criadaPorInvalidoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> configuracao().criadaPor(null).build());
            assertThrows(IllegalArgumentException.class, () -> configuracao().criadaPor(" ").build());
        }
    }

    @Nested
    @DisplayName("Transições manuais")
    final class TransicoesManuais {

        @Test
        @DisplayName("ciclo completo registra cada transição com autor e horário, em ordem")
        void cicloCompletoRegistraEventosEmOrdem() {
            Votacao votacao = configuracao().criadaPor(ORGANIZADOR).build();
            relogio.avancarMinutos(1);
            votacao.alterarRegra(new MaioriaAbsoluta(false), ORGANIZADOR);
            relogio.avancarMinutos(1);
            votacao.abrir(ORGANIZADOR);
            votacao.registrarVoto("e1", "Chapa A");
            relogio.avancarMinutos(1);
            votacao.encerrar("mesario");
            relogio.avancarMinutos(1);
            votacao.apurar("apurador");

            assertEquals(List.of(
                    new EventoAuditoria(CRIACAO, ORGANIZADOR, TipoEvento.CRIADA, ""),
                    new EventoAuditoria(CRIACAO.plusSeconds(60), ORGANIZADOR, TipoEvento.REGRA_ALTERADA,
                            new MaioriaAbsoluta(false).descricao()),
                    new EventoAuditoria(CRIACAO.plusSeconds(120), ORGANIZADOR, TipoEvento.ABERTA, ""),
                    new EventoAuditoria(CRIACAO.plusSeconds(180), "mesario", TipoEvento.ENCERRADA, ""),
                    new EventoAuditoria(CRIACAO.plusSeconds(240), "apurador", TipoEvento.APURADA, "Eleito: Chapa A")),
                    votacao.auditoria());
        }

        @Test
        @DisplayName("versões sem autor registram \"sistema\"")
        void versoesSemAutorUsamSistema() {
            Votacao votacao = configuracao().criadaPor(ORGANIZADOR).build();
            votacao.alterarRegra(new MaioriaSimples());
            votacao.abrir();
            votacao.encerrar();
            votacao.apurar();

            List<String> autores = votacao.auditoria().stream().map(EventoAuditoria::autor).toList();
            assertEquals(List.of(ORGANIZADOR, SISTEMA, SISTEMA, SISTEMA, SISTEMA), autores);
        }

        @Test
        @DisplayName("APURADA traz a situação no detalhe (ex.: sem vencedor)")
        void apuradaTrazSituacao() {
            Votacao votacao = configuracao().build();
            votacao.abrir();
            votacao.encerrar();
            votacao.apurar();

            assertEquals("Sem vencedor", ultimo(votacao).detalhe());
        }

        @Test
        @DisplayName("apurar de novo devolve o mesmo resultado sem registrar outro evento")
        void apurarDeNovoNaoRegistraOutroEvento() {
            Votacao votacao = configuracao().build();
            votacao.abrir();
            votacao.encerrar();
            votacao.apurar(ORGANIZADOR);
            votacao.apurar("outra pessoa");

            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA, TipoEvento.ENCERRADA, TipoEvento.APURADA),
                    tipos(votacao));
        }
    }

    @Nested
    @DisplayName("Operação proibida ou inválida não gera evento")
    final class OperacaoProibida {

        @Test
        @DisplayName("encerrar e apurar em rascunho são recusados sem registrar evento")
        void transicaoProibidaNaoRegistra() {
            Votacao votacao = configuracao().build();

            assertThrows(IllegalStateException.class, () -> votacao.encerrar(ORGANIZADOR));
            assertThrows(IllegalStateException.class, () -> votacao.apurar(ORGANIZADOR));

            assertEquals(List.of(TipoEvento.CRIADA), tipos(votacao));
        }

        @Test
        @DisplayName("alterar a regra com a votação aberta é recusado sem registrar evento")
        void alterarRegraAbertaNaoRegistra() {
            Votacao votacao = configuracao().build();
            votacao.abrir();

            assertThrows(IllegalStateException.class, () -> votacao.alterarRegra(new MaioriaSimples(), ORGANIZADOR));
            assertThrows(IllegalStateException.class, () -> votacao.abrir(ORGANIZADOR));

            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA), tipos(votacao));
        }

        @Test
        @DisplayName("regra nula é recusada sem registrar evento")
        void regraNulaNaoRegistra() {
            Votacao votacao = configuracao().build();

            assertThrows(IllegalArgumentException.class, () -> votacao.alterarRegra(null, ORGANIZADOR));

            assertEquals(List.of(TipoEvento.CRIADA), tipos(votacao));
        }

        @Test
        @DisplayName("autor vazio é recusado antes da transição: o estado não muda")
        void autorVazioNaoMudaEstado() {
            Votacao votacao = configuracao().build();

            assertThrows(IllegalArgumentException.class, () -> votacao.abrir(" "));
            RegraApuracao regraOriginal = votacao.regra();
            assertThrows(IllegalArgumentException.class, () -> votacao.alterarRegra(new MaioriaSimples(), null));
            assertSame(regraOriginal, votacao.regra());
            assertEquals("rascunho", votacao.status());

            votacao.abrir();
            assertThrows(IllegalArgumentException.class, () -> votacao.encerrar(""));
            assertEquals("aberta", votacao.status());

            votacao.encerrar();
            assertThrows(IllegalArgumentException.class, () -> votacao.apurar(null));
            assertEquals("encerrada", votacao.status());
            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA, TipoEvento.ENCERRADA), tipos(votacao));
        }
    }

    @Nested
    @DisplayName("Transições pelo período")
    final class PeloPeriodo {

        @Test
        @DisplayName("abertura e encerramento automáticos registram \"sistema\" e o motivo")
        void transicoesAutomaticasRegistramSistema() {
            PeriodoVotacao periodo = new PeriodoVotacao(CRIACAO.plusSeconds(600), CRIACAO.plusSeconds(1200));
            Votacao votacao = configuracao().criadaPor(ORGANIZADOR).periodo(periodo).build();

            relogio.avancarMinutos(10);
            votacao.atualizarPeloRelogio();
            relogio.avancarMinutos(10);
            votacao.atualizarPeloRelogio();

            List<EventoAuditoria> eventos = votacao.auditoria();
            assertEquals(new EventoAuditoria(CRIACAO.plusSeconds(600), SISTEMA, TipoEvento.ABERTA, "inicio do periodo"),
                    eventos.get(1));
            assertEquals(new EventoAuditoria(CRIACAO.plusSeconds(1200), SISTEMA, TipoEvento.ENCERRADA,
                    "fim do periodo"), eventos.get(2));
            assertEquals(3, eventos.size());
        }

        @Test
        @DisplayName("período inteiro já passado: abre e encerra de uma vez, nessa ordem")
        void periodoPassadoAbreEEncerra() {
            PeriodoVotacao periodo = new PeriodoVotacao(CRIACAO.plusSeconds(60), CRIACAO.plusSeconds(120));
            Votacao votacao = configuracao().periodo(periodo).build();

            relogio.avancarMinutos(5);
            votacao.atualizarPeloRelogio();

            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA, TipoEvento.ENCERRADA), tipos(votacao));
        }
    }

    @Nested
    @DisplayName("Sigilo (RNF-01)")
    final class SigiloDoVoto {

        @Test
        @DisplayName("votos não geram evento nem aparecem no detalhe, mesmo em voto identificado")
        void votosNaoAparecemNaAuditoria() {
            Votacao votacao = configuracao().sigilo(Sigilo.IDENTIFICADO).build();
            votacao.abrir();
            votacao.registrarVoto("e1", "Chapa B");
            votacao.registrarVoto("e2", "Chapa B");

            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA), tipos(votacao));
            assertTrue(votacao.auditoria().stream()
                    .noneMatch(evento -> evento.detalhe().contains("e1") || evento.detalhe().contains("Chapa B")));
        }
    }

    @Test
    @DisplayName("a lista da auditoria é imutável")
    void auditoriaImutavel() {
        Votacao votacao = configuracao().build();

        assertThrows(UnsupportedOperationException.class, () -> votacao.auditoria().clear());
    }
}
