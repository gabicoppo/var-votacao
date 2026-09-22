package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PontuacaoTop3Test {

    @Test
    void primeiroLugarValePontosDaPosicaoVezesPesoDaCategoria() {
        List<VotoCategoria> votos = List.of(new VotoCategoria("Ana", 1, 2.0));

        Map<String, Double> pontos = PontuacaoTop3.calcularPontos(votos);

        assertEquals(10.0, pontos.get("Ana"));
    }

    @Test
    void segundoETerceiroLugarUsamSuasPontuacoesBase() {
        List<VotoCategoria> votos = List.of(
                new VotoCategoria("Bruno", 2, 1.0),
                new VotoCategoria("Carla", 3, 1.0));

        Map<String, Double> pontos = PontuacaoTop3.calcularPontos(votos);

        assertEquals(3.0, pontos.get("Bruno"));
        assertEquals(1.0, pontos.get("Carla"));
    }

    @Test
    void votosDoMesmoCandidatoEmCategoriasDiferentesSaoSomados() {
        List<VotoCategoria> votos = List.of(
                new VotoCategoria("Ana", 1, 1.0),
                new VotoCategoria("Ana", 2, 1.0));

        Map<String, Double> pontos = PontuacaoTop3.calcularPontos(votos);

        assertEquals(8.0, pontos.get("Ana"));
    }

    @Test
    void candidatosDiferentesTemPontuacoesIndependentes() {
        List<VotoCategoria> votos = List.of(
                new VotoCategoria("Ana", 1, 1.0),
                new VotoCategoria("Bruno", 1, 1.0));

        Map<String, Double> pontos = PontuacaoTop3.calcularPontos(votos);

        assertEquals(5.0, pontos.get("Ana"));
        assertEquals(5.0, pontos.get("Bruno"));
    }

    @Test
    void pesoDaCategoriaPonderaOsPontos() {
        List<VotoCategoria> votos = List.of(new VotoCategoria("Ana", 1, 0.5));

        Map<String, Double> pontos = PontuacaoTop3.calcularPontos(votos);

        assertEquals(2.5, pontos.get("Ana"));
    }

    @Test
    void listaDeVotosVaziaRetornaMapaVazio() {
        Map<String, Double> pontos = PontuacaoTop3.calcularPontos(List.of());

        assertTrue(pontos.isEmpty());
    }

    @Test
    void posicaoForaDeUmATresLancaExcecao() {
        List<VotoCategoria> votos = List.of(new VotoCategoria("Ana", 4, 1.0));

        assertThrows(IllegalArgumentException.class, () -> PontuacaoTop3.calcularPontos(votos));
    }
}
