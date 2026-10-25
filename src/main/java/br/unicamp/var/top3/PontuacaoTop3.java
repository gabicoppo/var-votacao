package br.unicamp.var.top3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo de pontos do Top 3 ponderado: cada voto de categoria dá pontos ao
 * candidato conforme a posição (1º = 5, 2º = 3, 3º = 1 ponto, definidos em
 * {@link Posicao}), multiplicados pelo peso da categoria. Os pontos são
 * somados por candidato.
 *
 * <p>Escopo desta classe: apenas o cálculo de pontos. Não há ranking final
 * nem critério de desempate; isso fica fora deste escopo.</p>
 */
public final class PontuacaoTop3 {

    private PontuacaoTop3() {
        // Classe utilitária, não deve ser instanciada.
    }

    /**
     * Calcula os pontos de cada candidato a partir dos votos de categoria.
     *
     * @param votos lista de votos de categoria; cada voto atribui uma posição
     *              a um candidato, ponderada pelo peso da categoria
     * @return mapa com o total de pontos acumulado por candidato
     */
    public static Map<String, Double> calcularPontos(List<VotoCategoria> votos) {
        Map<String, Double> pontosPorCandidato = new HashMap<>();
        for (VotoCategoria voto : votos) {
            double pontos = voto.posicao().pontos() * voto.pesoCategoria();
            pontosPorCandidato.merge(voto.candidato(), pontos, Double::sum);
        }
        return pontosPorCandidato;
    }
}
