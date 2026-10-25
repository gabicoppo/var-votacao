package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PosicaoTest {

    @Test
    void cadaPosicaoValeSeusPontosBase() {
        assertEquals(5, Posicao.PRIMEIRO.pontos());
        assertEquals(3, Posicao.SEGUNDO.pontos());
        assertEquals(1, Posicao.TERCEIRO.pontos());
    }

    @Test
    void converteColocacaoNumericaNaPosicao() {
        assertEquals(Posicao.PRIMEIRO, Posicao.daColocacao(1));
        assertEquals(Posicao.SEGUNDO, Posicao.daColocacao(2));
        assertEquals(Posicao.TERCEIRO, Posicao.daColocacao(3));
    }

    @Test
    void colocacaoDaPosicaoIdaEVolta() {
        for (Posicao posicao : Posicao.values()) {
            assertEquals(posicao, Posicao.daColocacao(posicao.colocacao()));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, -1, 7})
    void colocacaoForaDeUmATresLancaExcecao(int colocacao) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> Posicao.daColocacao(colocacao));

        assertEquals("posicao deve ser 1, 2 ou 3, mas foi " + colocacao, erro.getMessage());
    }
}
