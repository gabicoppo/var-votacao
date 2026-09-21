package br.unicamp.var.apuracao2turno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApuracaoMajoritariaTest {

    @Test
    void candidatoVenceComMaioriaAbsolutaClara() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 65);
        votos.put("Fabricio", 35);

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
}