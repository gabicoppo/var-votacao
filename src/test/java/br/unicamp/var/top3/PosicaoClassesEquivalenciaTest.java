package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teste funcional de {@link Posicao} por particionamento em classes de
 * equivalência (Atividade 5, C1). Entrada: {@code colocacao} de
 * {@link Posicao#daColocacao(int)}. O domínio válido é o intervalo 1..3, com
 * uma classe inválida de cada lado.
 *
 * <pre>
 * | ID  | Entrada   | Classe de equivalência                                     | Válida?  |
 * |-----|-----------|------------------------------------------------------------|----------|
 * | CE1 | colocacao | 1, 2 ou 3 → PRIMEIRO, SEGUNDO, TERCEIRO (5, 3 e 1 pontos)  | válida   |
 * | CE2 | colocacao | menor que 1 → IllegalArgumentException                     | inválida |
 * | CE3 | colocacao | maior que 3 → IllegalArgumentException                     | inválida |
 * </pre>
 *
 * <p>Reforço de valor limite nas fronteiras entre CE1, CE2 e CE3 (a análise
 * de valor limite principal do C2 é a da maioria qualificada):</p>
 *
 * <pre>
 * | ID  | Valor | Fronteira                  | Resultado esperado |
 * |-----|-------|----------------------------|--------------------|
 * | VL1 | 0     | maior valor de CE2         | rejeitado          |
 * | VL2 | 1     | menor valor de CE1         | PRIMEIRO           |
 * | VL3 | 3     | maior valor de CE1         | TERCEIRO           |
 * | VL4 | 4     | menor valor de CE3         | rejeitado          |
 * </pre>
 */
class PosicaoClassesEquivalenciaTest {

    private static final String MENSAGEM_INVALIDA = "posicao deve ser 1, 2 ou 3, mas foi ";

    @Nested
    @DisplayName("Entrada: colocacao")
    class EntradaColocacao {

        @ParameterizedTest
        @CsvSource({"1, PRIMEIRO, 5", "2, SEGUNDO, 3", "3, TERCEIRO, 1"})
        @DisplayName("CE1 (válida): colocação de 1 a 3 vira a posição com seus pontos base")
        void ce1ConverteColocacaoNaPosicaoComSeusPontos(int colocacao, Posicao esperada, int pontos) {
            Posicao posicao = Posicao.daColocacao(colocacao);

            assertEquals(esperada, posicao);
            assertEquals(pontos, posicao.pontos());
            assertEquals(colocacao, posicao.colocacao());
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -50, Integer.MIN_VALUE})
        @DisplayName("CE2 (inválida): colocação menor que 1 é rejeitada")
        void ce2RejeitaColocacaoMenorQueUm(int colocacao) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Posicao.daColocacao(colocacao));

            assertEquals(MENSAGEM_INVALIDA + colocacao, erro.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {7, 100, Integer.MAX_VALUE})
        @DisplayName("CE3 (inválida): colocação maior que 3 é rejeitada")
        void ce3RejeitaColocacaoMaiorQueTres(int colocacao) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Posicao.daColocacao(colocacao));

            assertEquals(MENSAGEM_INVALIDA + colocacao, erro.getMessage());
        }
    }

    @Nested
    @DisplayName("Limites de colocacao (reforço de valor limite)")
    class LimitesColocacao {

        @Test
        @DisplayName("VL1 (inválido): 0, logo abaixo do domínio, é rejeitado")
        void vl1RejeitaZero() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Posicao.daColocacao(0));

            assertEquals(MENSAGEM_INVALIDA + 0, erro.getMessage());
        }

        @Test
        @DisplayName("VL2 (válido): 1, menor valor do domínio, vira PRIMEIRO")
        void vl2AceitaUm() {
            assertEquals(Posicao.PRIMEIRO, Posicao.daColocacao(1));
        }

        @Test
        @DisplayName("VL3 (válido): 3, maior valor do domínio, vira TERCEIRO")
        void vl3AceitaTres() {
            assertEquals(Posicao.TERCEIRO, Posicao.daColocacao(3));
        }

        @Test
        @DisplayName("VL4 (inválido): 4, logo acima do domínio, é rejeitado")
        void vl4RejeitaQuatro() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> Posicao.daColocacao(4));

            assertEquals(MENSAGEM_INVALIDA + 4, erro.getMessage());
        }
    }
}
