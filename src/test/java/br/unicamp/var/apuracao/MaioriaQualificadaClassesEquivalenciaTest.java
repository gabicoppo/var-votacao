package br.unicamp.var.apuracao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Particionamento em classes de equivalência de {@link MaioriaQualificada}
 * (construtor e {@link MaioriaQualificada#apurar}), história #21, issue #43.
 *
 * <p>Votantes é a soma dos votos da {@link ContagemVotos}. A mais votada conta
 * como "a favor" e as demais opções, somadas, como "contra". A validação da
 * própria {@link ContagemVotos} fica fora do escopo ({@code ContagemVotosTest}).</p>
 *
 * <table>
 *   <caption>Classes de equivalência</caption>
 *   <tr><th>ID</th><th>Entrada</th><th>Classe</th><th>Válida?</th><th>Esperado</th></tr>
 *   <tr><td>CE1</td><td>quorumMinimo</td><td>em (0, 1]</td><td>válida</td><td>cria a regra</td></tr>
 *   <tr><td>CE2</td><td>quorumMinimo</td><td>&le; 0 (inclui -&infin;)</td><td>inválida</td>
 *       <td>IllegalArgumentException</td></tr>
 *   <tr><td>CE3</td><td>quorumMinimo</td><td>&gt; 1 (inclui +&infin;)</td><td>inválida</td>
 *       <td>IllegalArgumentException</td></tr>
 *   <tr><td>CE4</td><td>quorumMinimo</td><td>NaN</td><td>inválida</td><td>IllegalArgumentException</td></tr>
 *   <tr><td>CE5</td><td>fracaoExigida</td><td>em (0, 1]</td><td>válida</td><td>cria a regra</td></tr>
 *   <tr><td>CE6</td><td>fracaoExigida</td><td>&le; 0 (inclui -&infin;)</td><td>inválida</td>
 *       <td>IllegalArgumentException</td></tr>
 *   <tr><td>CE7</td><td>fracaoExigida</td><td>&gt; 1 (inclui +&infin;)</td><td>inválida</td>
 *       <td>IllegalArgumentException</td></tr>
 *   <tr><td>CE8</td><td>fracaoExigida</td><td>NaN</td><td>inválida</td><td>IllegalArgumentException</td></tr>
 *   <tr><td>CE9</td><td>participação</td><td>votantes &gt; 0 e abaixo do quórum</td><td>válida</td>
 *       <td>QuorumNaoAtingido</td></tr>
 *   <tr><td>CE10</td><td>participação</td><td>no quórum ou acima</td><td>válida</td>
 *       <td>segue para a disputa e a fração</td></tr>
 *   <tr><td>CE11</td><td>participação</td><td>zero votos</td><td>válida</td>
 *       <td>QuorumNaoAtingido (aprovada nunca recebe 0 votos válidos)</td></tr>
 *   <tr><td>CE12</td><td>1º lugar</td><td>mais votada única</td><td>válida</td><td>segue para a fração</td></tr>
 *   <tr><td>CE13</td><td>1º lugar</td><td>empate no 1º lugar (2 ou 3 opções)</td><td>válida</td>
 *       <td>SemVencedor, mesmo que a fração fosse atingida</td></tr>
 *   <tr><td>CE14</td><td>1º lugar</td><td>empate só abaixo da mais votada, antes ou depois dela</td>
 *       <td>válida</td><td>não atrapalha: segue para a fração</td></tr>
 *   <tr><td>CE15</td><td>fração</td><td>mais votada &ge; fração exigida</td><td>válida</td><td>Eleito</td></tr>
 *   <tr><td>CE16</td><td>fração</td><td>mais votada &lt; fração exigida</td><td>válida</td><td>SemVencedor</td></tr>
 *   <tr><td>CE17</td><td>nº de opções</td><td>uma opção</td><td>válida</td>
 *       <td>100% dos válidos: Eleito se houver quórum</td></tr>
 *   <tr><td>CE18</td><td>nº de opções</td><td>duas opções</td><td>válida</td><td>a outra opção é o "contra"</td></tr>
 *   <tr><td>CE19</td><td>nº de opções</td><td>três ou mais</td><td>válida</td>
 *       <td>as demais, somadas, são o "contra"</td></tr>
 * </table>
 *
 * <p>Os limites entre essas classes estão em {@link MaioriaQualificadaValorLimiteTest}.</p>
 */
final class MaioriaQualificadaClassesEquivalenciaTest {

    private static final double DOIS_TERCOS = 2.0 / 3;
    private static final double QUORUM_PADRAO = 0.5;
    private static final String[] NOMES = {"Aprovar", "Rejeitar", "Adiar"};

    /** Regra típica do estatuto: metade dos aptos votando e 2/3 dos votos válidos. */
    private final RegraApuracao regra = new MaioriaQualificada(QUORUM_PADRAO, DOIS_TERCOS);

    private static ContagemVotos contagem(int totalAptos, int... votos) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < votos.length; i++) {
            mapa.put(NOMES[i], votos[i]);
        }
        return new ContagemVotos(mapa, totalAptos);
    }

    private static void assertEleito(String esperado, ResultadoApuracao resultado) {
        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals(esperado, eleito.candidato());
    }

    @Nested
    @DisplayName("Entrada quorumMinimo (construtor)")
    final class QuorumMinimo {

        @ParameterizedTest(name = "quorumMinimo = {0}")
        @ValueSource(doubles = {0.25, 0.5, 0.75})
        @DisplayName("CE1 (válida): quorumMinimo em (0, 1]")
        void quorumNoIntervaloCriaRegra(double quorumMinimo) {
            assertDoesNotThrow(() -> new MaioriaQualificada(quorumMinimo, DOIS_TERCOS));
        }

        @ParameterizedTest(name = "quorumMinimo = {0}")
        @ValueSource(doubles = {-0.5, -1.0, Double.NEGATIVE_INFINITY})
        @DisplayName("CE2 (inválida): quorumMinimo negativo")
        void quorumNegativoLancaExcecao(double quorumMinimo) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(quorumMinimo, DOIS_TERCOS));
            assertEquals("quorumMinimo deve estar em (0, 1]", erro.getMessage());
        }

        @ParameterizedTest(name = "quorumMinimo = {0}")
        @ValueSource(doubles = {1.01, 2.0, Double.POSITIVE_INFINITY})
        @DisplayName("CE3 (inválida): quorumMinimo acima de 1")
        void quorumAcimaDeUmLancaExcecao(double quorumMinimo) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(quorumMinimo, DOIS_TERCOS));
            assertEquals("quorumMinimo deve estar em (0, 1]", erro.getMessage());
        }

        @Test
        @DisplayName("CE4 (inválida): quorumMinimo NaN")
        void quorumNaoNumericoLancaExcecao() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(Double.NaN, DOIS_TERCOS));
            assertEquals("quorumMinimo deve estar em (0, 1]", erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Entrada fracaoExigida (construtor)")
    final class FracaoExigida {

        @ParameterizedTest(name = "fracaoExigida = {0}")
        @ValueSource(doubles = {0.5, DOIS_TERCOS, 0.75})
        @DisplayName("CE5 (válida): fracaoExigida em (0, 1]")
        void fracaoNoIntervaloCriaRegra(double fracaoExigida) {
            assertDoesNotThrow(() -> new MaioriaQualificada(QUORUM_PADRAO, fracaoExigida));
        }

        @ParameterizedTest(name = "fracaoExigida = {0}")
        @ValueSource(doubles = {-0.5, -1.0, Double.NEGATIVE_INFINITY})
        @DisplayName("CE6 (inválida): fracaoExigida negativa")
        void fracaoNegativaLancaExcecao(double fracaoExigida) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(QUORUM_PADRAO, fracaoExigida));
            assertEquals("fracaoExigida deve estar em (0, 1]", erro.getMessage());
        }

        @ParameterizedTest(name = "fracaoExigida = {0}")
        @ValueSource(doubles = {1.5, 2.0, Double.POSITIVE_INFINITY})
        @DisplayName("CE7 (inválida): fracaoExigida acima de 1")
        void fracaoAcimaDeUmLancaExcecao(double fracaoExigida) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(QUORUM_PADRAO, fracaoExigida));
            assertEquals("fracaoExigida deve estar em (0, 1]", erro.getMessage());
        }

        @Test
        @DisplayName("CE8 (inválida): fracaoExigida NaN")
        void fracaoNaoNumericaLancaExcecao() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(QUORUM_PADRAO, Double.NaN));
            assertEquals("fracaoExigida deve estar em (0, 1]", erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Entrada participação (votantes / aptos, quórum de 50%)")
    final class Participacao {

        @ParameterizedTest(name = "{0} x {1} de 100 aptos")
        @CsvSource({"1, 0", "20, 5", "30, 10"})
        @DisplayName("CE9 (válida): votantes abaixo do quórum")
        void abaixoDoQuorumNaoApura(int aprovar, int rejeitar) {
            ResultadoApuracao resultado = regra.apurar(contagem(100, aprovar, rejeitar));

            assertInstanceOf(ResultadoApuracao.QuorumNaoAtingido.class, resultado);
        }

        @ParameterizedTest(name = "{0} x {1} de 100 aptos")
        @CsvSource({"60, 10", "90, 10", "100, 0"})
        @DisplayName("CE10 (válida): votantes no quórum ou acima seguem para a fração")
        void acimaDoQuorumSegueParaFracao(int aprovar, int rejeitar) {
            ResultadoApuracao resultado = regra.apurar(contagem(100, aprovar, rejeitar));

            assertEleito("Aprovar", resultado);
        }

        @Test
        @DisplayName("CE11 (válida): nenhum voto não atinge o quórum")
        void semVotosQuorumNaoAtingido() {
            ResultadoApuracao resultado = regra.apurar(contagem(100, 0, 0));

            assertInstanceOf(ResultadoApuracao.QuorumNaoAtingido.class, resultado);
        }
    }

    @Nested
    @DisplayName("Entrada disputa pelo 1º lugar (fração exigida baixa, de 30%)")
    final class PrimeiroLugar {

        private final RegraApuracao fracaoBaixa = new MaioriaQualificada(QUORUM_PADRAO, 0.3);

        @Test
        @DisplayName("CE12 (válida): mais votada única segue para a fração")
        void maisVotadaUnicaSegueParaFracao() {
            ResultadoApuracao resultado = fracaoBaixa.apurar(contagem(100, 60, 30));

            assertEleito("Aprovar", resultado);
        }

        @ParameterizedTest(name = "votos = {0}, {1}, {2}")
        @CsvSource({"40, 40, 0", "30, 30, 30", "45, 45, 10", "10, 45, 45"})
        @DisplayName("CE13 (válida): empate no 1º lugar fica sem vencedor mesmo com a fração atingida")
        void empateNoPrimeiroLugarFicaSemVencedor(int aprovar, int rejeitar, int adiar) {
            ResultadoApuracao resultado = fracaoBaixa.apurar(contagem(100, aprovar, rejeitar, adiar));

            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        }

        @ParameterizedTest(name = "votos = {0}, {1}, {2} -> {3}")
        @CsvSource({"70, 10, 10, Aprovar", "10, 10, 70, Adiar", "10, 70, 10, Rejeitar"})
        @DisplayName("CE14 (válida): empate abaixo da mais votada não impede a eleição")
        void empateAbaixoDoPrimeiroNaoAtrapalha(int aprovar, int rejeitar, int adiar, String vencedora) {
            ResultadoApuracao resultado = fracaoBaixa.apurar(contagem(100, aprovar, rejeitar, adiar));

            assertEleito(vencedora, resultado);
        }
    }

    @Nested
    @DisplayName("Entrada fração da mais votada (exigida: 2/3)")
    final class FracaoDaMaisVotada {

        @Test
        @DisplayName("CE15 (válida): mais votada com a fração exigida é eleita")
        void maisVotadaComDoisTercosVence() {
            ResultadoApuracao resultado = regra.apurar(contagem(100, 70, 20));

            assertEleito("Aprovar", resultado);
        }

        @Test
        @DisplayName("CE16 (válida): mais votada abaixo da fração exigida fica sem vencedor")
        void fracaoNaoAtingidaFicaSemVencedor() {
            ResultadoApuracao resultado = regra.apurar(contagem(100, 60, 40));

            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        }
    }

    @Nested
    @DisplayName("Entrada número de opções (exigida: 2/3)")
    final class NumeroDeOpcoes {

        @Test
        @DisplayName("CE17 (válida): opção única tem 100% dos válidos e é eleita")
        void opcaoUnicaComQuorumVence() {
            ResultadoApuracao resultado = regra.apurar(contagem(100, 50));

            assertEleito("Aprovar", resultado);
        }

        @Test
        @DisplayName("CE18 (válida): com duas opções, a outra conta como contra")
        void segundaOpcaoContaComoContra() {
            // 65 de 100 válidos fica abaixo de 2/3 por causa dos 35 de Rejeitar
            ResultadoApuracao resultado = regra.apurar(contagem(100, 65, 35));

            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        }

        @ParameterizedTest(name = "votos = {0}, {1}, {2} -> eleita = {3}")
        @CsvSource({"60, 20, 10, true", "60, 25, 15, false"})
        @DisplayName("CE19 (válida): com três ou mais opções, as demais somadas contam como contra")
        void demaisOpcoesSomadasContamComoContra(int aprovar, int rejeitar, int adiar, boolean eleita) {
            // 60/90 atinge os 2/3; 60/100 não, embora nenhuma outra opção sozinha tenha 1/3
            ResultadoApuracao resultado = regra.apurar(contagem(100, aprovar, rejeitar, adiar));

            if (eleita) {
                assertEleito("Aprovar", resultado);
            } else {
                assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
            }
        }
    }
}
