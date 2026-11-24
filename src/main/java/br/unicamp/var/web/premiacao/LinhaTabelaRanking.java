package br.unicamp.var.web.premiacao;

import java.util.List;

/**
 * Linha da tabela completa do ranking (história #30), já formatada.
 *
 * @param colocacao      colocação, dividida em caso de empate
 * @param candidato      nome
 * @param pontos         pontuação total, ex.: {@code "23,5"}
 * @param primeiros      vezes em que foi escolhido em 1º
 * @param segundos       vezes em 2º
 * @param terceiros      vezes em 3º
 * @param pontosPorGrupo pontos dados por cada grupo, na ordem dos grupos (transparência, D15 e D16)
 */
public record LinhaTabelaRanking(int colocacao, String candidato, String pontos, int primeiros, int segundos,
                                 int terceiros, List<String> pontosPorGrupo) {

    /** Guarda uma cópia imutável dos pontos por grupo. */
    public LinhaTabelaRanking {
        pontosPorGrupo = List.copyOf(pontosPorGrupo);
    }
}
