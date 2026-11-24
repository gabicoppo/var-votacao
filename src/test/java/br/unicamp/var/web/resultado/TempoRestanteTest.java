package br.unicamp.var.web.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unicamp.var.processo.PeriodoVotacao;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste do texto de prazo (história #20) e da participação antes da apuração, por classes de
 * equivalência de status e valores limite de duração.
 */
class TempoRestanteTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T12:00:00Z");
    private static final PeriodoVotacao PERIODO =
            new PeriodoVotacao(AGORA.plus(Duration.ofHours(2)), AGORA.plus(Duration.ofDays(3)));

    @Nested
    @DisplayName("Texto por status")
    class PorStatus {

        @Test
        @DisplayName("Rascunho: sem período, antes do início e com o início já passado")
        void rascunho() {
            assertEquals("Ainda não foi aberta pelo organizador.", TempoRestante.texto("rascunho", null, AGORA));
            assertEquals("Abre em 2 h", TempoRestante.texto("rascunho", PERIODO, AGORA));
            assertEquals("Abre em instantes.",
                    TempoRestante.texto("rascunho", PERIODO, AGORA.plus(Duration.ofHours(3))));
        }

        @Test
        @DisplayName("Aberta: sem período, dentro dele e com o fim já passado")
        void aberta() {
            assertEquals("Sem prazo definido: o organizador encerra a votação.",
                    TempoRestante.texto("aberta", null, AGORA));
            assertEquals("Encerra em 3 dias", TempoRestante.texto("aberta", PERIODO, AGORA));
            assertEquals("Encerra em instantes.",
                    TempoRestante.texto("aberta", PERIODO, AGORA.plus(Duration.ofDays(4))));
        }

        @Test
        @DisplayName("Encerrada aguarda a apuração; apurada não tem prazo")
        void encerradaEApurada() {
            assertEquals("Votação encerrada. Aguardando a apuração.", TempoRestante.texto("encerrada", PERIODO, AGORA));
            assertEquals("", TempoRestante.texto("apurada", PERIODO, AGORA));
        }
    }

    @ParameterizedTest
    @CsvSource({"PT0S, menos de 1 min", "PT59S, menos de 1 min", "PT1M, 1 min", "PT59M, 59 min", "PT1H, 1 h",
        "PT1H5M, 1 h e 5 min", "PT23H59M, 23 h e 59 min", "PT24H, 1 dia", "PT25H, 1 dia e 1 h", "PT50H, 2 dias e 2 h"})
    @DisplayName("Duração com no máximo duas unidades (limites de minuto, hora e dia)")
    void duracao(Duration duracao, String esperado) {
        assertEquals(esperado, TempoRestante.duracao(duracao));
    }

    @Test
    @DisplayName("Participação: 0 aptos dá 0%; mais votantes que aptos não passa de 100%")
    void participacao() {
        assertEquals(new Andamento("aberta", 0, 0, 0, "0%", ""), Andamento.de("aberta", 0, 0, ""));
        assertEquals("58,3%", Andamento.de("aberta", 7, 12, "").participacaoTexto());
        assertEquals(100.0, Andamento.de("aberta", 5, 4, "").largura());
        assertEquals(new AndamentoJson("aberta", 7, 12), Andamento.de("aberta", 7, 12, "").json());
    }
}
