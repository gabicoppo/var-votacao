package br.unicamp.var.web.eleitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Prazos em texto, com valor limite: exatamente 1 h, meia-noite e {@code fim == agora}. Horários em
 * UTC; em America/Sao_Paulo, 2026-11-24T13:00Z são 10:00 do dia 24.
 */
class TempoRestanteTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final Instant AGORA = Instant.parse("2026-11-24T13:00:00Z");

    @Nested
    @DisplayName("descrever (fim)")
    class Fim {

        @ParameterizedTest(name = "fim {0} -> {1}")
        @CsvSource(delimiter = '|', value = {
            "2026-11-24T13:00:00Z|encerrada",
            "2026-11-24T12:59:59Z|encerrada",
            "2026-11-24T13:00:01Z|encerra em 1 min",
            "2026-11-24T13:25:00Z|encerra em 25 min",
            "2026-11-24T13:59:59Z|encerra em 60 min",
            "2026-11-24T14:00:00Z|encerra hoje às 11:00",
            "2026-11-24T21:00:00Z|encerra hoje às 18:00",
            "2026-11-25T02:59:00Z|encerra hoje às 23:59",
            "2026-11-25T03:00:00Z|encerra amanhã às 00:00",
            "2026-11-25T21:00:00Z|encerra amanhã às 18:00",
            "2026-11-26T03:00:00Z|encerra em 2 dias",
            "2026-11-27T21:00:00Z|encerra em 3 dias",
        })
        @DisplayName("abaixo / no / acima de cada limite")
        void limites(String fim, String esperado) {
            assertEquals(esperado, TempoRestante.descrever(AGORA, Instant.parse(fim), SP));
        }

        @Test
        @DisplayName("o dia é o do calendário no fuso informado, não o de UTC")
        void diaNoFusoInformado() {
            Instant noiteEmSp = Instant.parse("2026-11-25T01:00:00Z"); // 22:00 do dia 24 em SP
            assertEquals("encerra hoje às 22:00", TempoRestante.descrever(AGORA, noiteEmSp, SP));
            assertEquals("encerra amanhã às 01:00", TempoRestante.descrever(AGORA, noiteEmSp, ZoneId.of("UTC")));
        }
    }

    @Nested
    @DisplayName("descreverInicio")
    class Inicio {

        @ParameterizedTest(name = "início {0} -> {1}")
        @CsvSource(delimiter = '|', value = {
            "2026-11-24T13:00:00Z|já começou",
            "2026-11-24T13:10:00Z|começa em 10 min",
            "2026-11-24T14:00:00Z|começa hoje às 11:00",
            "2026-11-25T12:00:00Z|começa amanhã às 09:00",
            "2026-11-26T12:00:00Z|começa em 2 dias",
        })
        @DisplayName("mesmas regras, com o verbo \"começa\"")
        void limites(String inicio, String esperado) {
            assertEquals(esperado, TempoRestante.descreverInicio(AGORA, Instant.parse(inicio), SP));
        }
    }
}
