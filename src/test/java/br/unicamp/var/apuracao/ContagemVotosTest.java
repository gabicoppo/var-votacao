package br.unicamp.var.apuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContagemVotosTest {

    @Test
    void somaVotosValidosDeTodasAsOpcoes() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 40, "Fabricio", 35), 100);

        assertEquals(75, contagem.totalVotosValidos());
    }

    @Test
    void preservaOrdemDeInsercaoDasOpcoes() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Juliana", 1);
        votos.put("Gabriela", 2);
        votos.put("Fabricio", 3);

        ContagemVotos contagem = new ContagemVotos(votos, 10);

        assertEquals(List.of("Juliana", "Gabriela", "Fabricio"), List.copyOf(contagem.votosPorOpcao().keySet()));
    }

    @Test
    void alteracaoNoMapaOriginalNaoAfetaContagem() {
        Map<String, Integer> votos = new LinkedHashMap<>();
        votos.put("Gabriela", 5);

        ContagemVotos contagem = new ContagemVotos(votos, 10);
        votos.put("Fabricio", 3);

        assertEquals(Map.of("Gabriela", 5), contagem.votosPorOpcao());
    }

    @Test
    void mapaDaContagemEImutavel() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 5), 10);

        assertThrows(UnsupportedOperationException.class, () -> contagem.votosPorOpcao().put("Fabricio", 1));
    }

    @Test
    void aceitaTodosOsAptosVotando() {
        ContagemVotos contagem = new ContagemVotos(Map.of("Gabriela", 6, "Fabricio", 4), 10);

        assertEquals(10, contagem.totalVotosValidos());
    }

    @Test
    void mapaNuloLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new ContagemVotos(null, 10));
    }

    @Test
    void mapaVazioLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new ContagemVotos(Map.of(), 10));
    }

    @Test
    void opcaoNulaLancaExcecao() {
        Map<String, Integer> votos = new HashMap<>();
        votos.put(null, 1);

        assertThrows(IllegalArgumentException.class, () -> new ContagemVotos(votos, 10));
    }

    @Test
    void votosNulosLancamExcecao() {
        Map<String, Integer> votos = new HashMap<>();
        votos.put("Gabriela", null);

        assertThrows(IllegalArgumentException.class, () -> new ContagemVotos(votos, 10));
    }

    @Test
    void votosNegativosLancamExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new ContagemVotos(Map.of("Gabriela", -1), 10));
    }

    @Test
    void totalAptosZeroLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () -> new ContagemVotos(Map.of("Gabriela", 0), 0));
    }

    @Test
    void somaDosVotosAcimaDosAptosLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContagemVotos(Map.of("Gabriela", 6, "Fabricio", 5), 10));
    }
}
