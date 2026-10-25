package br.unicamp.var.top3;

/**
 * Posição que um candidato pode ocupar num voto do Top 3, com os pontos base
 * que ela vale (1º = 5, 2º = 3, 3º = 1 ponto).
 *
 * <p>Só existem essas três posições: qualquer outra colocação é rejeitada ao
 * converter o número em {@code Posicao}.</p>
 */
public enum Posicao {

    PRIMEIRO(1, 5),
    SEGUNDO(2, 3),
    TERCEIRO(3, 1);

    private final int colocacao;
    private final int pontos;

    Posicao(int colocacao, int pontos) {
        this.colocacao = colocacao;
        this.pontos = pontos;
    }

    /**
     * Converte a colocação numérica na posição correspondente.
     *
     * @param colocacao colocação do candidato no voto (1, 2 ou 3)
     * @return a posição correspondente
     * @throws IllegalArgumentException se a colocação não for 1, 2 ou 3
     */
    public static Posicao daColocacao(int colocacao) {
        for (Posicao posicao : values()) {
            if (posicao.colocacao == colocacao) {
                return posicao;
            }
        }
        throw new IllegalArgumentException("posicao deve ser 1, 2 ou 3, mas foi " + colocacao);
    }

    /**
     * Retorna a colocação numérica da posição.
     *
     * @return 1, 2 ou 3
     */
    public int colocacao() {
        return colocacao;
    }

    /**
     * Retorna os pontos base da posição, antes da ponderação pelo peso.
     *
     * @return pontos base da posição
     */
    public int pontos() {
        return pontos;
    }
}
