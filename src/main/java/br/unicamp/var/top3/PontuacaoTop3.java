package br.unicamp.var.top3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo de pontos do Top 3 ponderado: cada voto de categoria dá pontos ao
 * candidato conforme a posição (1º = 5, 2º = 3, 3º = 1 ponto), multiplicados
 * pelo peso da categoria. Os pontos são somados por candidato.
 *
 * <p>Escopo desta classe: apenas o cálculo de pontos. Não há ranking final
 * nem critério de desempate; isso fica fora deste escopo.</p>
 */
public final class PontuacaoTop3 {

    private static final int PONTOS_PRIMEIRO_LUGAR = 5;
    private static final int PONTOS_SEGUNDO_LUGAR = 3;
    private static final int PONTOS_TERCEIRO_LUGAR = 1;

    private PontuacaoTop3() {
        // Classe utilitária, não deve ser instanciada.
    }

    /**
     * Calcula os pontos de cada candidato a partir dos votos de categoria.
     *
     * @param votos lista de votos de categoria; cada voto atribui uma posição
     *              (1, 2 ou 3) a um candidato, ponderada pelo peso da categoria
     * @return mapa com o total de pontos acumulado por candidato
     * @throws IllegalArgumentException se algum voto tiver posição fora de 1, 2 ou 3
     */
    public static Map<String, Double> calcularPontos(List<VotoCategoria> votos) {
        Map<String, Double> pontosPorCandidato = new HashMap<>();
        for (VotoCategoria voto : votos) {
            double pontos = pontosDaPosicao(voto.posicao()) * voto.pesoCategoria();
            pontosPorCandidato.merge(voto.candidato(), pontos, Double::sum);
        }
        return pontosPorCandidato;
    }

    private static int pontosDaPosicao(int posicao) {
        return switch (posicao) {
            case 1 -> PONTOS_PRIMEIRO_LUGAR;
            case 2 -> PONTOS_SEGUNDO_LUGAR;
            case 3 -> PONTOS_TERCEIRO_LUGAR;
            default -> throw new IllegalArgumentException("posicao deve ser 1, 2 ou 3, mas foi " + posicao);
        };
    }
}
