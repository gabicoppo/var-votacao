package br.unicamp.var.top3;

import java.util.Map;

/**
 * Resultado de um candidato no ranking ponderado (#30).
 *
 * @param colocacao colocação compartilhada em caso de empate esportivo
 * @param candidato nome do candidato
 * @param pontos total ponderado de todos os grupos
 * @param primeirosLugares número de cédulas que o escolheram em primeiro
 * @param segundosLugares número de cédulas que o escolheram em segundo
 * @param terceirosLugares número de cédulas que o escolheram em terceiro
 * @param pontosPorCategoria contribuição de cada grupo de eleitores (#23), inclusive as iguais a zero
 */
public record LinhaRanking(int colocacao, String candidato, double pontos, int primeirosLugares,
                           int segundosLugares, int terceirosLugares, Map<String, Double> pontosPorCategoria) {

    /** Guarda uma cópia imutável da pontuação por grupo (#30). */
    public LinhaRanking {
        pontosPorCategoria = Map.copyOf(ValidacaoTop3.presente(pontosPorCategoria, "mapa de pontos"));
    }
}
