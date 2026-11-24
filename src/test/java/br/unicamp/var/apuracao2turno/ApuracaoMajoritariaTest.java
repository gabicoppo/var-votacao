package br.unicamp.var.apuracao2turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApuracaoMajoritariaTest {

    @Test
    void candidatoVenceComMaioriaAbsolutaClara() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 67);
        votos.put("Fabricio", 33);

        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    @Test
    void candidatoComExatos50PorCentoNaoEConsideradoEleito() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 50);
        votos.put("Fabricio", 50);

        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos);

        assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
    }

    @Test
    void semMaioriaComTresCandidatosRetornaDoisMaisVotados() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 40);
        votos.put("Fabricio", 35);
        votos.put("Juliana", 25);

        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos);

        ResultadoApuracao.SegundoTurno segundoTurno =
                assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
        assertEquals("Gabriela", segundoTurno.primeiroColocado());
        assertEquals("Fabricio", segundoTurno.segundoColocado());
    }

    @Test
    void doisMaisVotadosVemNaOrdemCorretaIndependenteDaOrdemDeInsercao() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Juliana", 20);
        votos.put("Gabriela", 45);
        votos.put("Fabricio", 35);

        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos);

        ResultadoApuracao.SegundoTurno segundoTurno =
                assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
        assertEquals("Gabriela", segundoTurno.primeiroColocado());
        assertEquals("Fabricio", segundoTurno.segundoColocado());
    }

    @Test
    void doisCandidatosEmpatadosVaoParaSegundoTurno() {
        // Com exatamente dois candidatos, a única forma de nenhum atingir
        // maioria absoluta é um empate exato (50%/50%). Empate no primeiro
        // lugar é uma limitação conhecida e está fora do escopo desta
        // feature, então aqui só verificamos que o resultado é SegundoTurno,
        // sem assumir qual dos dois aparece como "primeiro colocado".
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 100);
        votos.put("Fabricio", 100);

        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos);

        assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
    }

    @Test
    void candidatoUnicoComTodosOsVotosEEleitoDiretamente() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 100);

        ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    private static Map<String, Integer> votos(Object... nomesEVotos) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < nomesEVotos.length; i += 2) {
            mapa.put((String) nomesEVotos[i], (Integer) nomesEVotos[i + 1]);
        }
        return mapa;
    }

    private static void assertSegundoTurno(String primeiro, String segundo, ResultadoApuracao resultado) {
        ResultadoApuracao.SegundoTurno turno = assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
        assertEquals(primeiro, turno.primeiroColocado());
        assertEquals(segundo, turno.segundoColocado());
    }

    @Nested
    @DisplayName("Sem votos válidos")
    final class SemVotos {

        @Test
        @DisplayName("zero votos para dois candidatos não leva a 2º turno (0/0 não pode virar NaN)")
        void zeroVotosComDoisCandidatosFicaSemVencedor() {
            ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos("Chapa A", 0, "Chapa B", 0));

            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        }

        @Test
        @DisplayName("candidato único sem votos fica sem vencedor, sem IndexOutOfBounds")
        void candidatoUnicoSemVotosFicaSemVencedor() {
            ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos("Chapa A", 0));

            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        }

        @Test
        @DisplayName("mapa sem candidatos fica sem vencedor")
        void mapaVazioFicaSemVencedor() {
            ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(Map.of());

            assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
        }
    }

    @Test
    @DisplayName("mapa nulo lança exceção")
    void mapaNuloLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> ApuracaoMajoritaria.apurar(null));
    }

    @Nested
    @DisplayName("Candidato único")
    final class CandidatoUnico {

        @Test
        @DisplayName("com um voto já é eleito (100% dos válidos)")
        void candidatoUnicoComUmVotoEEleito() {
            ResultadoApuracao resultado = ApuracaoMajoritaria.apurar(votos("Chapa A", 1));

            ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
            assertEquals("Chapa A", eleito.candidato());
        }
    }

    @Nested
    @DisplayName("Empates: vale a ordem de cadastro (ordem do mapa)")
    final class Empates {

        @Test
        @DisplayName("empate no 2º lugar: vai ao 2º turno quem foi cadastrado antes (B antes de C)")
        void empateNoSegundoLugarFavoreceQuemVemAntes() {
            assertSegundoTurno("A", "B", ApuracaoMajoritaria.apurar(votos("A", 40, "B", 30, "C", 30)));
        }

        @Test
        @DisplayName("empate no 2º lugar: invertendo o cadastro, quem vai é C")
        void empateNoSegundoLugarComOrdemInvertida() {
            assertSegundoTurno("A", "C", ApuracaoMajoritaria.apurar(votos("C", 30, "B", 30, "A", 40)));
        }

        @Test
        @DisplayName("empate no 1º lugar: o primeiro cadastrado aparece como primeiro colocado")
        void empateNoPrimeiroLugarSegueOrdemDeCadastro() {
            assertSegundoTurno("B", "A", ApuracaoMajoritaria.apurar(votos("C", 20, "B", 40, "A", 40)));
        }
    }
}
