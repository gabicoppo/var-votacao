package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CedulaTop3Test {

    @Nested
    @DisplayName("Preencher a cédula")
    class Construcao {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("Exige nome nas três posições")
        void rejeitaNomeInvalido(String nome) {
            assertThrows(IllegalArgumentException.class, () -> new CedulaTop3(nome, "B", "C"));
            assertThrows(IllegalArgumentException.class, () -> new CedulaTop3("A", nome, "C"));
            assertThrows(IllegalArgumentException.class, () -> new CedulaTop3("A", "B", nome));
        }

        @ParameterizedTest
        @CsvSource({"A,A,C", "A,B,A", "A,B,B"})
        @DisplayName("Rejeita repetição entre quaisquer posições")
        void rejeitaRepeticao(String primeiro, String segundo, String terceiro) {
            assertThrows(IllegalArgumentException.class, () -> new CedulaTop3(primeiro, segundo, terceiro));
        }
    }

    @Nested
    @DisplayName("Converter em votos ponderados")
    class ComPeso {

        @ParameterizedTest
        @ValueSource(doubles = {1, 1.25})
        @DisplayName("Aplica o mesmo peso às três posições e preserva a ordem")
        void converteNaOrdem(double peso) {
            CedulaTop3 cedula = new CedulaTop3("C", "A", "B");

            assertEquals(List.of(new VotoCategoria("C", Posicao.PRIMEIRO, peso),
                    new VotoCategoria("A", Posicao.SEGUNDO, peso),
                    new VotoCategoria("B", Posicao.TERCEIRO, peso)), cedula.comPeso(peso));
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("Rejeita peso inválido na conversão")
        void rejeitaPesoInvalido(double peso) {
            CedulaTop3 cedula = new CedulaTop3("A", "B", "C");

            assertThrows(IllegalArgumentException.class, () -> cedula.comPeso(peso));
        }
    }
}
