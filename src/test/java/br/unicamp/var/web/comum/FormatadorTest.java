package br.unicamp.var.web.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unicamp.var.top3.StatusPremiacao;
import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Formatação dos templates ({@code @fmt}): datas no fuso de Brasília e números pt-BR, qualquer que
 * seja o locale ou o fuso da máquina.
 */
class FormatadorTest {

    private final Formatador fmt = new Formatador();

    @Nested
    @DisplayName("dataHora")
    class DataHora {

        @Test
        @DisplayName("converte o instante UTC para America/Sao_Paulo")
        void noFusoDeBrasilia() {
            assertEquals("24/11/2026 18:30", fmt.dataHora(Instant.parse("2026-11-24T21:30:00Z")));
        }

        @Test
        @DisplayName("independe do fuso padrão da máquina")
        void independeDoFusoDaMaquina() {
            TimeZone original = TimeZone.getDefault();
            try {
                TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
                assertEquals("01/01/2027 09:00", fmt.dataHora(Instant.parse("2027-01-01T12:00:00Z")));
            } finally {
                TimeZone.setDefault(original);
            }
        }

        @Test
        @DisplayName("nulo vira travessão")
        void nulo() {
            assertEquals("—", fmt.dataHora(null));
        }
    }

    @Nested
    @DisplayName("Números")
    class Numeros {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = ';', value = {"0.6667; 66,7%", "0.5; 50%", "1.0; 100%", "0; 0%", "0.0625; 6,3%"})
        @DisplayName("percentual com no máximo uma casa e vírgula")
        void percentual(double fracao, String esperado) {
            assertEquals(esperado, fmt.percentual(fracao));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = ';', value = {"12.5; 12,5", "12.0; 12", "1234.56; 1234,6", "-2.25; -2,3"})
        @DisplayName("número com no máximo uma casa e vírgula")
        void numero(double valor, String esperado) {
            assertEquals(esperado, fmt.numero(valor));
        }

        @Test
        @DisplayName("usa vírgula mesmo com o locale da máquina em inglês")
        void independeDoLocale() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.US);
                assertEquals("66,7%", fmt.percentual(2.0 / 3));
            } finally {
                Locale.setDefault(original);
            }
        }
    }

    @Nested
    @DisplayName("Status")
    class Status {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({"rascunho, Rascunho", "aberta, Aberta", "encerrada, Encerrada", "apurada, Apurada"})
        @DisplayName("status da votação com inicial maiúscula")
        void statusVotacao(String status, String esperado) {
            assertEquals(esperado, fmt.statusVotacao(status));
        }

        @Test
        @DisplayName("status de votação nulo ou vazio vira travessão")
        void statusVotacaoAusente() {
            assertEquals("—", fmt.statusVotacao(null));
            assertEquals("—", fmt.statusVotacao(" "));
        }

        @Test
        @DisplayName("status da premiação")
        void statusPremiacao() {
            assertEquals("Rascunho", fmt.statusPremiacao(StatusPremiacao.RASCUNHO));
            assertEquals("Aberta", fmt.statusPremiacao(StatusPremiacao.ABERTA));
            assertEquals("Encerrada", fmt.statusPremiacao(StatusPremiacao.ENCERRADA));
            assertEquals("Apurada", fmt.statusPremiacao(StatusPremiacao.APURADA));
            assertEquals("—", fmt.statusPremiacao(null));
        }
    }
}
