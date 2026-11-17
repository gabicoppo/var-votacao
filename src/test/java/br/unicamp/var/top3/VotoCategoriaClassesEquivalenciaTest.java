package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teste funcional de {@link VotoCategoria} por particionamento em classes de
 * equivalência (Atividade 5, C1). Entradas: {@code posicao}, {@code candidato}
 * e {@code pesoCategoria} dos construtores.
 *
 * <pre>
 * | ID   | Entrada       | Classe de equivalência                                   | Válida?  |
 * |------|---------------|----------------------------------------------------------|----------|
 * | CE4  | posicao       | enum Posicao, ou int de 1 a 3 convertido pelo construtor | válida   |
 * |      |               | auxiliar                                                 |          |
 * | CE5  | posicao       | null → NullPointerException                              | inválida |
 * | CE6  | posicao       | int fora de 1..3 → IllegalArgumentException              | inválida |
 * | CE7  | candidato     | nome com texto                                           | válida   |
 * | CE8  | candidato     | null, vazio ou só espaços → IllegalArgumentException     | inválida |
 * | CE9  | pesoCategoria | finito e maior que zero, inteiro ou fracionário          | válida   |
 * | CE10 | pesoCategoria | zero (0.0 ou -0.0) → IllegalArgumentException            | inválida |
 * | CE11 | pesoCategoria | negativo → IllegalArgumentException                      | inválida |
 * | CE12 | pesoCategoria | NaN, +∞ ou -∞ → IllegalArgumentException                 | inválida |
 * </pre>
 */
class VotoCategoriaClassesEquivalenciaTest {

    @Nested
    @DisplayName("Entrada: posicao")
    class EntradaPosicao {

        @ParameterizedTest
        @EnumSource(Posicao.class)
        @DisplayName("CE4 (válida): posição informada como enum")
        void ce4AceitaPosicaoComoEnum(Posicao posicao) {
            VotoCategoria voto = new VotoCategoria("Ana", posicao, 1.0);

            assertEquals(posicao, voto.posicao());
        }

        @ParameterizedTest
        @CsvSource({"1, PRIMEIRO", "2, SEGUNDO", "3, TERCEIRO"})
        @DisplayName("CE4 (válida): colocação int de 1 a 3 vira a posição correspondente")
        void ce4ConverteColocacaoIntNaPosicao(int colocacao, Posicao esperada) {
            VotoCategoria voto = new VotoCategoria("Ana", colocacao, 1.5);

            assertEquals(new VotoCategoria("Ana", esperada, 1.5), voto);
        }

        @Test
        @DisplayName("CE5 (inválida): posição nula é rejeitada")
        void ce5RejeitaPosicaoNula() {
            NullPointerException erro = assertThrows(NullPointerException.class,
                    () -> new VotoCategoria("Ana", null, 1.0));

            assertEquals("posicao", erro.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 7})
        @DisplayName("CE6 (inválida): colocação int fora de 1..3 é rejeitada")
        void ce6RejeitaColocacaoIntForaDoIntervalo(int colocacao) {
            assertThrows(IllegalArgumentException.class, () -> new VotoCategoria("Ana", colocacao, 1.0));
        }
    }

    @Nested
    @DisplayName("Entrada: candidato")
    class EntradaCandidato {

        @ParameterizedTest
        @ValueSource(strings = {"Ana", "Bruno Lima"})
        @DisplayName("CE7 (válida): candidato com nome é aceito")
        void ce7AceitaCandidatoComNome(String candidato) {
            VotoCategoria voto = new VotoCategoria(candidato, Posicao.PRIMEIRO, 1.0);

            assertEquals(candidato, voto.candidato());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("CE8 (inválida): candidato nulo, vazio ou só com espaços é rejeitado")
        void ce8RejeitaCandidatoSemNome(String candidatoInvalido) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new VotoCategoria(candidatoInvalido, Posicao.PRIMEIRO, 1.0));

            assertEquals("candidato nao pode ser nulo, vazio ou em branco", erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Entrada: pesoCategoria")
    class EntradaPesoCategoria {

        @ParameterizedTest
        @ValueSource(doubles = {1.0, 2.0, 0.5})
        @DisplayName("CE9 (válida): peso finito e maior que zero é aceito")
        void ce9AceitaPesoPositivo(double peso) {
            VotoCategoria voto = new VotoCategoria("Ana", Posicao.PRIMEIRO, peso);

            assertEquals(peso, voto.pesoCategoria());
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, -0.0})
        @DisplayName("CE10 (inválida): peso zero é rejeitado")
        void ce10RejeitaPesoZero(double peso) {
            assertPesoRejeitado(peso);
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1.0, -0.5})
        @DisplayName("CE11 (inválida): peso negativo é rejeitado")
        void ce11RejeitaPesoNegativo(double peso) {
            assertPesoRejeitado(peso);
        }

        @ParameterizedTest
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("CE12 (inválida): peso NaN ou infinito é rejeitado")
        void ce12RejeitaPesoNaoFinito(double peso) {
            assertPesoRejeitado(peso);
        }

        private void assertPesoRejeitado(double peso) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> new VotoCategoria("Ana", Posicao.PRIMEIRO, peso));

            assertEquals("pesoCategoria deve ser finito e maior que zero, mas foi " + peso, erro.getMessage());
        }
    }
}
