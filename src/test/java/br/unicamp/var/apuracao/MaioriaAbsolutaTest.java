package br.unicamp.var.apuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaioriaAbsolutaTest {

    private static ContagemVotos tresCandidatosSemMaioria() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 40);
        votos.put("Fabricio", 35);
        votos.put("Juliana", 25);
        return new ContagemVotos(votos, 100);
    }

    @Test
    void candidatoVenceComMaioriaAbsolutaClara() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 67, "Fabricio", 33), 100);

        ResultadoApuracao resultado = new MaioriaAbsoluta(false).apurar(contagem);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    @Test
    void cinquentaPorCentoMaisUmVotoEMaioriaAbsoluta() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 51, "Fabricio", 49), 100);

        ResultadoApuracao resultado = new MaioriaAbsoluta(false).apurar(contagem);

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    @Test
    void exatos50PorCentoNaoEMaioriaAbsolutaESemSegundoTurnoFicaSemVencedor() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 50);
        votos.put("Fabricio", 30);
        votos.put("Juliana", 20);

        ResultadoApuracao resultado = new MaioriaAbsoluta(false).apurar(new ContagemVotos(votos, 100));

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void semMaioriaComSegundoTurnoHabilitadoIndicaOsDoisMaisVotados() {
        ResultadoApuracao resultado = new MaioriaAbsoluta(true).apurar(tresCandidatosSemMaioria());

        ResultadoApuracao.SegundoTurno segundoTurno =
                assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
        assertEquals("Gabriela", segundoTurno.primeiroColocado());
        assertEquals("Fabricio", segundoTurno.segundoColocado());
    }

    @Test
    void semMaioriaComSegundoTurnoDesabilitadoFicaSemVencedor() {
        ResultadoApuracao resultado = new MaioriaAbsoluta(false).apurar(tresCandidatosSemMaioria());

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void empateComSegundoTurnoHabilitadoVaiParaSegundoTurno() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 50, "Fabricio", 50), 100);

        ResultadoApuracao resultado = new MaioriaAbsoluta(true).apurar(contagem);

        assertInstanceOf(ResultadoApuracao.SegundoTurno.class, resultado);
    }

    @Test
    void empateComSegundoTurnoDesabilitadoFicaSemVencedor() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 50, "Fabricio", 50), 100);

        ResultadoApuracao resultado = new MaioriaAbsoluta(false).apurar(contagem);

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void semVotosFicaSemVencedorMesmoComSegundoTurnoHabilitado() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 0, "Fabricio", 0), 100);

        ResultadoApuracao resultado = new MaioriaAbsoluta(true).apurar(contagem);

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }
}
