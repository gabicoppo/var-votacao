package br.unicamp.var.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Trilha de auditoria da votação (RNF-07): ordem, horário vindo do {@link Clock},
 * imutabilidade e validações.
 */
final class RegistroAuditoriaTest {

    private static final Instant INICIO = Instant.parse("2026-11-24T13:00:00Z");
    private static final String ORGANIZADOR = "organizador";

    /** Relógio de teste que avança um minuto a cada leitura. */
    private static final class RelogioQueAvanca extends Clock {
        private Instant atual = INICIO;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            Instant agora = atual;
            atual = atual.plus(Duration.ofMinutes(1));
            return agora;
        }
    }

    private final RegistroAuditoria registro = new RegistroAuditoria(Clock.fixed(INICIO, ZoneOffset.UTC));

    @Nested
    @DisplayName("Registro de eventos")
    final class Registro {

        @Test
        @DisplayName("registro novo começa vazio")
        void comecaVazio() {
            assertTrue(registro.eventos().isEmpty());
        }

        @Test
        @DisplayName("evento leva o horário do Clock, o autor, o tipo e o detalhe")
        void eventoLevaHorarioDoClock() {
            EventoAuditoria evento = registro.registrar(ORGANIZADOR, TipoEvento.APURADA, "Eleito: Chapa A");

            assertEquals(new EventoAuditoria(INICIO, ORGANIZADOR, TipoEvento.APURADA, "Eleito: Chapa A"), evento);
            assertEquals(List.of(evento), registro.eventos());
        }

        @Test
        @DisplayName("detalhe vazio é aceito")
        void detalheVazioEhAceito() {
            assertEquals("", registro.registrar(ORGANIZADOR, TipoEvento.ABERTA, "").detalhe());
        }

        @Test
        @DisplayName("eventos ficam na ordem em que foram registrados, em ordem cronológica")
        void eventosEmOrdemCronologica() {
            RegistroAuditoria comRelogio = new RegistroAuditoria(new RelogioQueAvanca());
            comRelogio.registrar("sistema", TipoEvento.CRIADA, "");
            comRelogio.registrar(ORGANIZADOR, TipoEvento.ABERTA, "");
            comRelogio.registrar("sistema", TipoEvento.ENCERRADA, "");

            List<EventoAuditoria> eventos = comRelogio.eventos();

            assertEquals(List.of(TipoEvento.CRIADA, TipoEvento.ABERTA, TipoEvento.ENCERRADA),
                    eventos.stream().map(EventoAuditoria::tipo).toList());
            assertEquals(List.of(INICIO, INICIO.plusSeconds(60), INICIO.plusSeconds(120)),
                    eventos.stream().map(EventoAuditoria::quando).toList());
        }
    }

    @Nested
    @DisplayName("Só de acréscimo (append-only)")
    final class Imutabilidade {

        @Test
        @DisplayName("a lista devolvida não pode ser alterada")
        void listaNaoPodeSerAlterada() {
            registro.registrar(ORGANIZADOR, TipoEvento.CRIADA, "");
            List<EventoAuditoria> eventos = registro.eventos();

            assertThrows(UnsupportedOperationException.class, eventos::clear);
            assertThrows(UnsupportedOperationException.class,
                    () -> eventos.add(new EventoAuditoria(INICIO, "intruso", TipoEvento.APURADA, "")));
        }

        @Test
        @DisplayName("a lista devolvida é uma cópia: eventos novos não aparecem nela")
        void listaEhCopia() {
            List<EventoAuditoria> antes = registro.eventos();
            registro.registrar(ORGANIZADOR, TipoEvento.CRIADA, "");

            assertTrue(antes.isEmpty());
            assertEquals(1, registro.eventos().size());
        }
    }

    @Nested
    @DisplayName("Sigilo do voto (RNF-01)")
    final class Sigilo {

        @Test
        @DisplayName("não existe tipo de evento para voto")
        void naoExisteEventoDeVoto() {
            assertTrue(Arrays.stream(TipoEvento.values()).noneMatch(tipo -> tipo.name().contains("VOTO")));
        }
    }

    @Nested
    @DisplayName("Validações")
    final class Validacoes {

        @Test
        @DisplayName("relógio nulo lança exceção")
        void relogioNuloLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new RegistroAuditoria(null));
        }

        @Test
        @DisplayName("autor nulo ou em branco lança exceção e não registra nada")
        void autorInvalidoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> registro.registrar(null, TipoEvento.CRIADA, ""));
            assertThrows(IllegalArgumentException.class, () -> registro.registrar("  ", TipoEvento.CRIADA, ""));
            assertTrue(registro.eventos().isEmpty());
        }

        @Test
        @DisplayName("tipo nulo lança exceção")
        void tipoNuloLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> registro.registrar(ORGANIZADOR, null, ""));
        }

        @Test
        @DisplayName("detalhe nulo lança exceção")
        void detalheNuloLancaExcecao() {
            assertThrows(IllegalArgumentException.class,
                    () -> registro.registrar(ORGANIZADOR, TipoEvento.CRIADA, null));
        }

        @Test
        @DisplayName("evento sem horário lança exceção")
        void eventoSemHorarioLancaExcecao() {
            assertThrows(IllegalArgumentException.class,
                    () -> new EventoAuditoria(null, ORGANIZADOR, TipoEvento.CRIADA, ""));
        }
    }
}
