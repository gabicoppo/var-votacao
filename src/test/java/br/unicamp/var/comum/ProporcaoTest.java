package br.unicamp.var.comum;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Classes de equivalência e valor limite de {@link Proporcao}, extraída das regras
 * de quórum, aprovação e maioria absoluta (histórias #21 e #29).
 *
 * <table>
 *   <caption>Classes de equivalência</caption>
 *   <tr><th>ID</th><th>Entrada</th><th>Classe</th><th>Válida?</th><th>Esperado</th></tr>
 *   <tr><td>CE1</td><td>total</td><td>&gt; 0</td><td>válida</td><td>cria a proporção</td></tr>
 *   <tr><td>CE2</td><td>total</td><td>= 0</td><td>válida</td>
 *       <td>valor 0; atinge e supera sempre falsos (sem NaN)</td></tr>
 *   <tr><td>CE3</td><td>total</td><td>&lt; 0</td><td>inválida</td><td>IllegalArgumentException</td></tr>
 *   <tr><td>CE4</td><td>parte</td><td>em [0, total]</td><td>válida</td><td>cria a proporção</td></tr>
 *   <tr><td>CE5</td><td>parte</td><td>&lt; 0</td><td>inválida</td><td>IllegalArgumentException</td></tr>
 *   <tr><td>CE6</td><td>parte</td><td>&gt; total</td><td>inválida</td><td>IllegalArgumentException</td></tr>
 *   <tr><td>CE7</td><td>atinge</td><td>valor &ge; limiar</td><td>válida</td><td>true</td></tr>
 *   <tr><td>CE8</td><td>atinge</td><td>valor &lt; limiar</td><td>válida</td><td>false</td></tr>
 *   <tr><td>CE9</td><td>supera</td><td>valor &gt; limiar</td><td>válida</td><td>true</td></tr>
 *   <tr><td>CE10</td><td>supera</td><td>valor &le; limiar</td><td>válida</td><td>false</td></tr>
 * </table>
 *
 * <table>
 *   <caption>Limites</caption>
 *   <tr><th>ID</th><th>Entrada</th><th>Abaixo / no limite / acima</th><th>Esperado</th></tr>
 *   <tr><td>VL1</td><td>total</td><td>-1 / 0 / 1</td><td>inválido / válido / válido</td></tr>
 *   <tr><td>VL2</td><td>parte</td><td>-1 / 0 / 1 (total 10)</td><td>inválido / válido / válido</td></tr>
 *   <tr><td>VL3</td><td>parte</td><td>total - 1 / total / total + 1</td><td>válido / válido / inválido</td></tr>
 *   <tr><td>VL4</td><td>atinge(0.5)</td><td>4 / 5 / 6 de 10</td><td>false / true / true</td></tr>
 *   <tr><td>VL5</td><td>supera(0.5)</td><td>4 / 5 / 6 de 10</td><td>false / false / true</td></tr>
 *   <tr><td>VL6</td><td>atinge(2/3)</td><td>19 / 20 / 21 de 30</td><td>false / true / true (20/30 == 2/3)</td></tr>
 * </table>
 */
final class ProporcaoTest {

    private static final double METADE = 0.5;
    private static final double DOIS_TERCOS = 2.0 / 3;

    @Nested
    @DisplayName("Validação no construtor")
    final class Construtor {

        @Test
        @DisplayName("CE1 + CE4: parte em [0, total] com total positivo é válida")
        void parteDentroDoTotalEhValida() {
            Proporcao proporcao = assertDoesNotThrow(() -> new Proporcao(3, 10));

            assertEquals(3, proporcao.parte());
            assertEquals(10, proporcao.total());
        }

        @Test
        @DisplayName("CE3: total negativo lança exceção")
        void totalNegativoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new Proporcao(0, -5));
        }

        @Test
        @DisplayName("CE5: parte negativa lança exceção")
        void parteNegativaLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new Proporcao(-3, 10));
        }

        @Test
        @DisplayName("CE6: parte maior que o total lança exceção")
        void parteMaiorQueTotalLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new Proporcao(15, 10));
        }

        @ParameterizedTest(name = "total = {0} -> válido? {1}")
        @CsvSource({"-1, false", "0, true", "1, true"})
        @DisplayName("VL1: total -1 / 0 / 1")
        void limiteDoTotal(int total, boolean valido) {
            assertValidade(valido, 0, total);
        }

        @ParameterizedTest(name = "parte = {0} de 10 -> válido? {1}")
        @CsvSource({"-1, false", "0, true", "1, true"})
        @DisplayName("VL2: parte -1 / 0 / 1 com total 10")
        void limiteInferiorDaParte(int parte, boolean valido) {
            assertValidade(valido, parte, 10);
        }

        @ParameterizedTest(name = "parte = {0} de 10 -> válido? {1}")
        @CsvSource({"9, true", "10, true", "11, false"})
        @DisplayName("VL3: parte total - 1 / total / total + 1")
        void limiteSuperiorDaParte(int parte, boolean valido) {
            assertValidade(valido, parte, 10);
        }

        private void assertValidade(boolean valido, int parte, int total) {
            if (valido) {
                assertDoesNotThrow(() -> new Proporcao(parte, total));
            } else {
                assertThrows(IllegalArgumentException.class, () -> new Proporcao(parte, total));
            }
        }
    }

    @Nested
    @DisplayName("Valor da proporção")
    final class Valor {

        @Test
        @DisplayName("CE1: divide sem truncar (1/3 não vira 0)")
        void divideSemTruncar() {
            assertEquals(1.0 / 3, new Proporcao(1, 3).valor());
        }

        @Test
        @DisplayName("CE2: total zero dá valor 0, e não NaN")
        void totalZeroDaValorZero() {
            assertEquals(0.0, new Proporcao(0, 0).valor());
        }

        @Test
        @DisplayName("CE4: parte igual ao total dá valor 1")
        void parteIgualAoTotalDaUm() {
            assertEquals(1.0, new Proporcao(7, 7).valor());
        }
    }

    @Nested
    @DisplayName("atinge (maior ou igual)")
    final class Atinge {

        @Test
        @DisplayName("CE7: valor acima do limiar atinge")
        void valorAcimaAtinge() {
            assertTrue(new Proporcao(8, 10).atinge(METADE));
        }

        @Test
        @DisplayName("CE8: valor abaixo do limiar não atinge")
        void valorAbaixoNaoAtinge() {
            assertFalse(new Proporcao(2, 10).atinge(METADE));
        }

        @Test
        @DisplayName("CE2: total zero não atinge nem limiar zero")
        void totalZeroNaoAtingeNemLimiarZero() {
            assertFalse(new Proporcao(0, 0).atinge(0.0));
        }

        @ParameterizedTest(name = "{0} de 10 -> {1}")
        @CsvSource({"4, false", "5, true", "6, true"})
        @DisplayName("VL4: atinge(0.5) com 4 / 5 / 6 de 10")
        void limiteDeAtingeMetade(int parte, boolean esperado) {
            assertEquals(esperado, new Proporcao(parte, 10).atinge(METADE));
        }

        @Test
        @DisplayName("VL6 (pré-condição): 20/30 é exatamente 2/3 em double")
        void vinteTrintaEhExatamenteDoisTercos() {
            assertEquals(DOIS_TERCOS, new Proporcao(20, 30).valor());
        }

        @ParameterizedTest(name = "{0} de 30 -> {1}")
        @CsvSource({"19, false", "20, true", "21, true"})
        @DisplayName("VL6: atinge(2/3) com 19 / 20 / 21 de 30")
        void limiteDeAtingeDoisTercos(int parte, boolean esperado) {
            assertEquals(esperado, new Proporcao(parte, 30).atinge(DOIS_TERCOS));
        }
    }

    @Nested
    @DisplayName("supera (estritamente maior)")
    final class Supera {

        @Test
        @DisplayName("CE9: valor acima do limiar supera")
        void valorAcimaSupera() {
            assertTrue(new Proporcao(8, 10).supera(METADE));
        }

        @Test
        @DisplayName("CE10: valor abaixo do limiar não supera")
        void valorAbaixoNaoSupera() {
            assertFalse(new Proporcao(2, 10).supera(METADE));
        }

        @Test
        @DisplayName("CE2: total zero não supera nem limiar negativo")
        void totalZeroNaoSuperaNemLimiarNegativo() {
            assertFalse(new Proporcao(0, 0).supera(-1.0));
        }

        @ParameterizedTest(name = "{0} de 10 -> {1}")
        @CsvSource({"4, false", "5, false", "6, true"})
        @DisplayName("VL5: supera(0.5) com 4 / 5 / 6 de 10 (exatamente a metade não supera)")
        void limiteDeSuperaMetade(int parte, boolean esperado) {
            assertEquals(esperado, new Proporcao(parte, 10).supera(METADE));
        }
    }
}
