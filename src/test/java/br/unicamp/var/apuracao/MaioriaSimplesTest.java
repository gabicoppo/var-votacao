package br.unicamp.var.apuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import br.unicamp.var.apuracao2turno.ResultadoApuracao;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaioriaSimplesTest {

    private final RegraApuracao regra = new MaioriaSimples();

    @Test
    void maisVotadaVenceSemPrecisarDeMaioriaAbsoluta() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Juliana", 25);
        votos.put("Gabriela", 40);
        votos.put("Fabricio", 35);

        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(votos, 100));

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    @Test
    void opcaoUnicaComVotosVence() {
        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(Map.of("Gabriela", 1), 100));

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    @Test
    void empateNoPrimeiroLugarFicaSemVencedor() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 40);
        votos.put("Fabricio", 40);
        votos.put("Juliana", 20);

        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(votos, 100));

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void empateNoSegundoLugarNaoImpedeVencedor() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Fabricio", 20);
        votos.put("Juliana", 20);
        votos.put("Gabriela", 30);

        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(votos, 100));

        ResultadoApuracao.Eleito eleito = assertInstanceOf(ResultadoApuracao.Eleito.class, resultado);
        assertEquals("Gabriela", eleito.candidato());
    }

    @Test
    void semVotosFicaSemVencedor() {
        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(Map.of("Gabriela", 0, "Fabricio", 0), 100));

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }

    @Test
    void opcaoUnicaSemVotosFicaSemVencedor() {
        ResultadoApuracao resultado = regra.apurar(new ContagemVotos(Map.of("Gabriela", 0), 100));

        assertInstanceOf(ResultadoApuracao.SemVencedor.class, resultado);
    }
}
