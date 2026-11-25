package br.unicamp.var.apuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Descrição legível de cada {@link RegraApuracao} (história #21; usada no relatório
 * da história #31 e na tela de configuração).
 */
final class DescricaoRegraTest {

    @Nested
    @DisplayName("Regra padrão")
    final class Padrao {

        @Test
        @DisplayName("regra criada por lambda usa a descrição padrão")
        void lambdaUsaDescricaoPadrao() {
            RegraApuracao regra = contagem -> new ResultadoApuracao.SemVencedor();

            assertEquals("Regra personalizada", regra.descricao());
        }
    }

    @Nested
    @DisplayName("Maioria simples")
    final class Simples {

        @Test
        @DisplayName("descreve a mais votada e o empate")
        void descreveMaioriaSimples() {
            assertEquals("Maioria simples: vence a opção mais votada; empate fica sem vencedor.",
                    new MaioriaSimples().descricao());
        }
    }

    @Nested
    @DisplayName("Maioria absoluta")
    final class Absoluta {

        @Test
        @DisplayName("com 2º turno habilitado")
        void comSegundoTurno() {
            assertEquals("Maioria absoluta: mais da metade dos votos válidos, "
                    + "com 2º turno entre os dois mais votados.", new MaioriaAbsoluta(true).descricao());
        }

        @Test
        @DisplayName("sem 2º turno")
        void semSegundoTurno() {
            assertEquals("Maioria absoluta: mais da metade dos votos válidos, sem 2º turno.",
                    new MaioriaAbsoluta(false).descricao());
        }
    }

    @Nested
    @DisplayName("Maioria qualificada")
    final class Qualificada {

        @Test
        @DisplayName("quórum de 50% e fração de 2/3 (66,7%)")
        void quorumMetadeEDoisTercos() {
            assertEquals("Maioria qualificada: quórum de 50% dos aptos e pelo menos 66,7% dos votos válidos.",
                    new MaioriaQualificada(0.5, 2.0 / 3).descricao());
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = ';',
                value = {"0.6; 60%", "1.0; 100%", "0.3333333333; 33,3%", "0.0625; 6,3%", "0.001; 0,1%"})
        @DisplayName("percentual com no máximo uma casa decimal e vírgula")
        void formataPercentual(double fracao, String esperado) {
            String descricao = new MaioriaQualificada(fracao, fracao).descricao();

            assertEquals("Maioria qualificada: quórum de " + esperado + " dos aptos e pelo menos "
                    + esperado + " dos votos válidos.", descricao);
        }

        @Test
        @DisplayName("usa vírgula decimal mesmo com o locale da máquina em inglês")
        void independeDoLocalePadrao() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.US);

                assertEquals("Maioria qualificada: quórum de 50% dos aptos e pelo menos 66,7% dos votos válidos.",
                        new MaioriaQualificada(0.5, 2.0 / 3).descricao());
            } finally {
                Locale.setDefault(original);
            }
        }
    }
}
