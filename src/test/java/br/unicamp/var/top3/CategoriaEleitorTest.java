package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CategoriaEleitorTest {

    @Nested
    @DisplayName("Criar grupo de votantes")
    class Construcao {

        @Test
        @DisplayName("Preserva o nome e o peso fracionário")
        void preservaNomeEPeso() {
            CategoriaEleitor grupo = new CategoriaEleitor("Público", 0.25);

            assertEquals("Público", grupo.nome());
            assertEquals(0.25, grupo.peso());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Rejeita grupo sem nome")
        void rejeitaNomeInvalido(String nome) {
            assertThrows(IllegalArgumentException.class, () -> new CategoriaEleitor(nome, 1));
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -0.0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("Rejeita peso não positivo ou não finito")
        void rejeitaPesoInvalido(double peso) {
            assertThrows(IllegalArgumentException.class, () -> new CategoriaEleitor("Público", peso));
        }
    }
}
