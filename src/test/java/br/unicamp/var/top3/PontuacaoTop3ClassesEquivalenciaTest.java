package br.unicamp.var.top3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Teste funcional de {@link PontuacaoTop3#calcularPontos(List)} por
 * particionamento em classes de equivalência (Atividade 5, C1). Entrada: a
 * lista {@code votos}, particionada pelo tamanho e por como os candidatos e as
 * posições se repetem nela.
 *
 * <pre>
 * | ID   | Entrada | Classe de equivalência                                          | Válida?  |
 * |------|---------|-----------------------------------------------------------------|----------|
 * | CE13 | votos   | lista vazia → mapa vazio                                        | válida   |
 * | CE14 | votos   | um voto → pontos da posição × peso da categoria                 | válida   |
 * | CE15 | votos   | mesmo candidato na mesma posição em várias categorias → soma    | válida   |
 * |      |         | ponderada                                                       |          |
 * | CE16 | votos   | candidatos distintos → pontuações independentes                 | válida   |
 * | CE17 | votos   | mesmo candidato em posições diferentes → soma dos pontos de     | válida   |
 * |      |         | cada posição                                                    |          |
 * | CE18 | votos   | lista null → NullPointerException                               | inválida |
 * | CE19 | votos   | lista com um voto null → NullPointerException                   | inválida |
 * </pre>
 */
class PontuacaoTop3ClassesEquivalenciaTest {

    @Nested
    @DisplayName("Entrada: votos (listas válidas)")
    class ListasValidas {

        @Test
        @DisplayName("CE13 (válida): lista vazia resulta em mapa vazio")
        void ce13ListaVaziaResultaEmMapaVazio() {
            Map<String, Double> pontos = PontuacaoTop3.calcularPontos(List.of());

            assertTrue(pontos.isEmpty());
        }

        @ParameterizedTest
        @CsvSource({"1, 2.0, 10.0", "2, 1.0, 3.0", "3, 1.0, 1.0", "1, 0.5, 2.5"})
        @DisplayName("CE14 (válida): um voto vale os pontos da posição vezes o peso da categoria")
        void ce14UmVotoValePontosDaPosicaoVezesPeso(int colocacao, double peso, double esperado) {
            List<VotoCategoria> votos = List.of(new VotoCategoria("Ana", colocacao, peso));

            assertEquals(Map.of("Ana", esperado), PontuacaoTop3.calcularPontos(votos));
        }

        @Test
        @DisplayName("CE15 (válida): mesmo candidato na mesma posição em várias categorias tem os pontos somados")
        void ce15MesmoCandidatoNaMesmaPosicaoSomaPontosPonderados() {
            List<VotoCategoria> votos = List.of(
                    new VotoCategoria("Ana", Posicao.PRIMEIRO, 1.0),
                    new VotoCategoria("Ana", Posicao.PRIMEIRO, 2.0));

            assertEquals(Map.of("Ana", 15.0), PontuacaoTop3.calcularPontos(votos));
        }

        @Test
        @DisplayName("CE16 (válida): candidatos distintos têm pontuações independentes")
        void ce16CandidatosDistintosTemPontuacoesIndependentes() {
            List<VotoCategoria> votos = List.of(
                    new VotoCategoria("Ana", Posicao.PRIMEIRO, 1.0),
                    new VotoCategoria("Bruno", Posicao.SEGUNDO, 1.0),
                    new VotoCategoria("Carla", Posicao.TERCEIRO, 1.0));

            assertEquals(Map.of("Ana", 5.0, "Bruno", 3.0, "Carla", 1.0), PontuacaoTop3.calcularPontos(votos));
        }

        @Test
        @DisplayName("CE17 (válida): mesmo candidato em posições diferentes soma os pontos de cada posição")
        void ce17MesmoCandidatoEmPosicoesDiferentesSomaPontos() {
            List<VotoCategoria> votos = List.of(
                    new VotoCategoria("Ana", Posicao.PRIMEIRO, 1.0),
                    new VotoCategoria("Ana", Posicao.SEGUNDO, 1.0));

            assertEquals(Map.of("Ana", 8.0), PontuacaoTop3.calcularPontos(votos));
        }
    }
}
