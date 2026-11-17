package br.unicamp.var.top3;

import java.util.Objects;

/**
 * Representa o voto de uma categoria do Top 3: um candidato colocado numa
 * posição (1º, 2º ou 3º), com o peso da categoria que fez esse voto.
 *
 * <p>Como a posição é uma {@link Posicao}, não é possível criar um voto com
 * colocação fora de 1, 2 ou 3.</p>
 *
 * <p>O peso precisa ser finito e maior que zero: peso zero excluiria o voto do
 * grupo (a história #23 proíbe), peso negativo inverteria o voto e
 * {@code NaN} ou infinito estragariam o total do candidato.</p>
 *
 * @param candidato      nome do candidato votado (não pode ser nulo, vazio ou em branco)
 * @param posicao        posição atribuída ao candidato nessa categoria
 * @param pesoCategoria  peso da categoria, usado para ponderar a pontuação (finito e maior que zero)
 */
public record VotoCategoria(String candidato, Posicao posicao, double pesoCategoria) {

    /**
     * Valida a posição, o candidato e o peso do voto.
     *
     * @throws NullPointerException     se a posição for nula
     * @throws IllegalArgumentException se o candidato for nulo, vazio ou em branco, ou se o peso
     *                                  não for finito e maior que zero
     */
    public VotoCategoria {
        Objects.requireNonNull(posicao, "posicao");
        if (candidato == null || candidato.isBlank()) {
            throw new IllegalArgumentException("candidato nao pode ser nulo, vazio ou em branco");
        }
        if (!Double.isFinite(pesoCategoria) || pesoCategoria <= 0) {
            throw new IllegalArgumentException(
                    "pesoCategoria deve ser finito e maior que zero, mas foi " + pesoCategoria);
        }
    }

    /**
     * Cria um voto a partir da colocação numérica do candidato.
     *
     * @param candidato      nome do candidato votado
     * @param posicao        colocação atribuída ao candidato (1, 2 ou 3)
     * @param pesoCategoria  peso da categoria, usado para ponderar a pontuação
     * @throws IllegalArgumentException se a colocação não for 1, 2 ou 3, ou se o candidato
     *                                  ou o peso forem inválidos
     */
    public VotoCategoria(String candidato, int posicao, double pesoCategoria) {
        this(candidato, Posicao.daColocacao(posicao), pesoCategoria);
    }
}
