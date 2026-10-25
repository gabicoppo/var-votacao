package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VotoCategoriaTest {

    @Test
    void colocacaoNumericaViraPosicao() {
        VotoCategoria voto = new VotoCategoria("Ana", 2, 1.5);

        assertEquals(Posicao.SEGUNDO, voto.posicao());
        assertEquals(new VotoCategoria("Ana", Posicao.SEGUNDO, 1.5), voto);
    }

    @Test
    void naoCriaVotoComColocacaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new VotoCategoria("Ana", 4, 1.0));
    }

    @Test
    void naoCriaVotoSemPosicao() {
        assertThrows(NullPointerException.class, () -> new VotoCategoria("Ana", null, 1.0));
    }
}
