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

/**
 * Análise de valor limite de {@link MaioriaQualificada}, história #21, issue #43.
 *
 * <p>Cada limite é testado logo abaixo, exatamente nele e logo acima. O quórum e
 * a fração usam {@code >=}, então o valor exato precisa passar. Os resultados
 * esperados são escritos como {@code QuorumNaoAtingido}, {@code SemVencedor} ou
 * {@code Eleito:<opção>}.</p>
 *
 * <table>
 *   <caption>Limites</caption>
 *   <tr><th>ID</th><th>Entrada</th><th>Configuração</th><th>Abaixo / no limite / acima</th><th>Esperado</th></tr>
 *   <tr><td>VL1</td><td>quórum</td><td>100 aptos, quorumMinimo = 0.5, uma opção</td>
 *       <td>49 / 50 / 51 votantes</td><td>QuorumNaoAtingido / Eleito / Eleito</td></tr>
 *   <tr><td>VL2</td><td>fração</td><td>fracaoExigida = 2/3, 30 votantes</td>
 *       <td>mais votada com 19 / 20 / 21</td><td>SemVencedor / Eleito / Eleito (20.0/30 == 2.0/3)</td></tr>
 *   <tr><td>VL3</td><td>fração</td><td>fracaoExigida = 0.6, 10 votantes</td>
 *       <td>mais votada com 5 / 6 / 7</td><td>SemVencedor / Eleito / Eleito</td></tr>
 *   <tr><td>VL4</td><td>quorumMinimo e fracaoExigida</td><td>limite inferior de (0, 1]</td>
 *       <td>0.0 / Math.nextUp(0.0)</td><td>inválido / válido (1 voto já atinge o quórum)</td></tr>
 *   <tr><td>VL5</td><td>quorumMinimo e fracaoExigida</td><td>limite superior de (0, 1]</td>
 *       <td>1.0 / Math.nextUp(1.0)</td><td>válido (unanimidade) / inválido</td></tr>
 *   <tr><td>VL6</td><td>diferença entre 1º e 2º</td><td>fracaoExigida = 0.5, duas opções</td>
 *       <td>A - B = -1 / 0 / +1</td><td>Eleito(B) / SemVencedor / Eleito(A)</td></tr>
 * </table>
 *
 * <p>As classes de equivalência separadas por esses limites estão em
 * {@link MaioriaQualificadaClassesEquivalenciaTest}.</p>
 */
final class MaioriaQualificadaValorLimiteTest {

    private static final double DOIS_TERCOS = 2.0 / 3;
    private static final double QUORUM_PADRAO = 0.5;
    private static final String[] NOMES = {"Aprovar", "Rejeitar", "Adiar"};
    private static final String PREFIXO_ELEITO = "Eleito:";

    private static ContagemVotos contagem(int totalAptos, int... votos) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < votos.length; i++) {
            mapa.put(NOMES[i], votos[i]);
        }
        return new ContagemVotos(mapa, totalAptos);
    }

    private static void assertResultado(String esperado, ResultadoApuracao resultado) {
        if (esperado.startsWith(PREFIXO_ELEITO)) {
            ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
            assertEquals(esperado.substring(PREFIXO_ELEITO.length()), eleito.candidato());
        } else if ("SemVencedor".equals(esperado)) {
            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        } else {
            assertEquals("QuorumNaoAtingido", esperado, "resultado esperado desconhecido");
            assertInstanceOf(ResultadoApuracao.QuorumNaoAtingido.class, resultado);
        }
    }

    @Nested
    @DisplayName("Limite do quórum")
    final class Quorum {

        @ParameterizedTest(name = "{0} votantes de 100 -> {1}")
        @CsvSource({"49, QuorumNaoAtingido", "50, Eleito:Aprovar", "51, Eleito:Aprovar"})
        @DisplayName("VL1: quórum de 50% com 100 aptos (49 / 50 / 51 votantes)")
        void quorumDeMetadeDosAptos(int votantes, String esperado) {
            RegraApuracao regra = new MaioriaQualificada(QUORUM_PADRAO, DOIS_TERCOS);

            assertResultado(esperado, regra.apurar(contagem(100, votantes)));
        }
    }

    @Nested
    @DisplayName("Limite da fração exigida")
    final class Fracao {

        @Test
        @DisplayName("VL2 (pré-condição): 20/30 é exatamente 2/3 em double")
        void vinteTrintaEhExatamenteDoisTercos() {
            assertEquals(DOIS_TERCOS, 20.0 / 30);
        }

        @ParameterizedTest(name = "{0} x {1} -> {2}")
        @CsvSource({"19, 11, SemVencedor", "20, 10, Eleito:Aprovar", "21, 9, Eleito:Aprovar"})
        @DisplayName("VL2: fração de 2/3 com 30 votantes (19 / 20 / 21)")
        void fracaoDeDoisTercos(int aprovar, int rejeitar, String esperado) {
            RegraApuracao regra = new MaioriaQualificada(QUORUM_PADRAO, DOIS_TERCOS);

            assertResultado(esperado, regra.apurar(contagem(30, aprovar, rejeitar)));
        }

        @ParameterizedTest(name = "{0} x {1} -> {2}")
        @CsvSource({"5, 5, SemVencedor", "6, 4, Eleito:Aprovar", "7, 3, Eleito:Aprovar"})
        @DisplayName("VL3: fração de 60% com 10 votantes (5 / 6 / 7)")
        void fracaoDeSessentaPorCento(int aprovar, int rejeitar, String esperado) {
            RegraApuracao regra = new MaioriaQualificada(QUORUM_PADRAO, 0.6);

            assertResultado(esperado, regra.apurar(contagem(10, aprovar, rejeitar)));
        }
    }

    @Nested
    @DisplayName("Limite inferior dos parâmetros do construtor")
    final class LimiteInferiorDoConstrutor {

        @Test
        @DisplayName("VL4 (inválido): quorumMinimo = 0.0")
        void quorumMinimoZeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(0.0, DOIS_TERCOS));
        }

        @Test
        @DisplayName("VL4 (inválido): fracaoExigida = 0.0")
        void fracaoExigidaZeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> new MaioriaQualificada(QUORUM_PADRAO, 0.0));
        }

        @Test
        @DisplayName("VL4 (válido): quorumMinimo = Math.nextUp(0.0), 1 voto já atinge o quórum")
        void menorQuorumPositivoBastaUmVoto() {
            RegraApuracao regra = assertDoesNotThrow(() -> new MaioriaQualificada(Math.nextUp(0.0), DOIS_TERCOS));

            assertResultado("QuorumNaoAtingido", regra.apurar(contagem(100, 0)));
            assertResultado("Eleito:Aprovar", regra.apurar(contagem(100, 1)));
        }

        @Test
        @DisplayName("VL4 (válido): fracaoExigida = Math.nextUp(0.0), qualquer mais votada única vence")
        void menorFracaoPositivaElegeMaisVotadaUnica() {
            RegraApuracao regra = assertDoesNotThrow(() -> new MaioriaQualificada(QUORUM_PADRAO, Math.nextUp(0.0)));

            assertResultado("Eleito:Aprovar", regra.apurar(contagem(100, 34, 33, 33)));
        }
    }

    @Nested
    @DisplayName("Limite superior dos parâmetros do construtor")
    final class LimiteSuperiorDoConstrutor {

        @ParameterizedTest(name = "{0} x {1} de 10 aptos -> {2}")
        @CsvSource({"10, 0, Eleito:Aprovar", "9, 0, QuorumNaoAtingido", "9, 1, SemVencedor"})
        @DisplayName("VL5 (válido): quorumMinimo = fracaoExigida = 1.0 exige unanimidade")
        void fracoesIguaisAUmExigemUnanimidade(int aprovar, int rejeitar, String esperado) {
            RegraApuracao unanimidade = new MaioriaQualificada(1.0, 1.0);

            assertResultado(esperado, unanimidade.apurar(contagem(10, aprovar, rejeitar)));
        }

        @Test
        @DisplayName("VL5 (inválido): quorumMinimo = Math.nextUp(1.0)")
        void quorumLogoAcimaDeUmLancaExcecao() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(Math.nextUp(1.0), DOIS_TERCOS));
        }

        @Test
        @DisplayName("VL5 (inválido): fracaoExigida = Math.nextUp(1.0)")
        void fracaoLogoAcimaDeUmLancaExcecao() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MaioriaQualificada(QUORUM_PADRAO, Math.nextUp(1.0)));
        }
    }

    @Nested
    @DisplayName("Limite da diferença entre o 1º e o 2º lugar")
    final class DiferencaEntrePrimeiroESegundo {

        @ParameterizedTest(name = "{0} x {1} -> {2}")
        @CsvSource({"40, 41, Eleito:Rejeitar", "40, 40, SemVencedor", "41, 40, Eleito:Aprovar"})
        @DisplayName("VL6: diferença de -1 / 0 / +1 voto com fração de 50%")
        void diferencaDeUmVotoDecide(int aprovar, int rejeitar, String esperado) {
            RegraApuracao metade = new MaioriaQualificada(QUORUM_PADRAO, 0.5);

            assertResultado(esperado, metade.apurar(contagem(100, aprovar, rejeitar)));
        }
    }
}
