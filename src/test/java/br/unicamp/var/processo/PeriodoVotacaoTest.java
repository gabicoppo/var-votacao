package br.unicamp.var.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link PeriodoVotacao} (história #20), com foco nos limites do intervalo
 * semiaberto {@code [inicio, fim)}.
 */
class PeriodoVotacaoTest {

    private static final Instant INICIO = Instant.parse("2027-03-01T08:00:00Z");
    private static final Instant FIM = Instant.parse("2027-03-01T18:00:00Z");
    private static final PeriodoVotacao PERIODO = new PeriodoVotacao(INICIO, FIM);

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("guarda início e fim")
        void guardaInicioEFim() {
            assertEquals(INICIO, PERIODO.inicio());
            assertEquals(FIM, PERIODO.fim());
        }

        @Test
        @DisplayName("aceita fim um nanossegundo depois do início")
        void aceitaFimLogoDepoisDoInicio() {
            PeriodoVotacao periodo = new PeriodoVotacao(INICIO, INICIO.plusNanos(1));

            assertTrue(periodo.contem(INICIO));
        }

        @Test
        @DisplayName("rejeita início nulo")
        void rejeitaInicioNulo() {
            assertThrows(IllegalArgumentException.class, () -> new PeriodoVotacao(null, FIM));
        }

        @Test
        @DisplayName("rejeita fim nulo")
        void rejeitaFimNulo() {
            assertThrows(IllegalArgumentException.class, () -> new PeriodoVotacao(INICIO, null));
        }

        @Test
        @DisplayName("rejeita fim igual ao início")
        void rejeitaFimIgualAoInicio() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new PeriodoVotacao(INICIO, INICIO));

            assertEquals("fim do periodo deve ser depois do inicio", erro.getMessage());
        }

        @Test
        @DisplayName("rejeita fim antes do início")
        void rejeitaFimAntesDoInicio() {
            assertThrows(IllegalArgumentException.class, () -> new PeriodoVotacao(FIM, INICIO));
        }
    }

    @Nested
    @DisplayName("Limites do intervalo [inicio, fim)")
    class Limites {

        @Test
        @DisplayName("antes do início: não começou, não contém e não terminou")
        void antesDoInicio() {
            Instant agora = INICIO.minusNanos(1);

            assertTrue(PERIODO.naoComecou(agora));
            assertFalse(PERIODO.contem(agora));
            assertFalse(PERIODO.terminou(agora));
        }

        @Test
        @DisplayName("no início exato: já começou e contém")
        void noInicioExato() {
            assertFalse(PERIODO.naoComecou(INICIO));
            assertTrue(PERIODO.contem(INICIO));
            assertFalse(PERIODO.terminou(INICIO));
        }

        @Test
        @DisplayName("um instante antes do fim: ainda contém")
        void logoAntesDoFim() {
            Instant agora = FIM.minusNanos(1);

            assertTrue(PERIODO.contem(agora));
            assertFalse(PERIODO.terminou(agora));
        }

        @Test
        @DisplayName("no fim exato: terminou e não contém")
        void noFimExato() {
            assertFalse(PERIODO.naoComecou(FIM));
            assertFalse(PERIODO.contem(FIM));
            assertTrue(PERIODO.terminou(FIM));
        }

        @Test
        @DisplayName("depois do fim: terminou e não contém")
        void depoisDoFim() {
            Instant agora = FIM.plusSeconds(1);

            assertFalse(PERIODO.contem(agora));
            assertTrue(PERIODO.terminou(agora));
        }
    }
}
