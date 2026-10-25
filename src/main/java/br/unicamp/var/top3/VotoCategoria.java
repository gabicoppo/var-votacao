package br.unicamp.var.top3;

import java.util.Objects;

/**
 * Representa o voto de uma categoria do Top 3: um candidato colocado numa
 * posição (1º, 2º ou 3º), com o peso da categoria que fez esse voto.
 *
 * <p>Como a posição é uma {@link Posicao}, não é possível criar um voto com
 * colocação fora de 1, 2 ou 3.</p>
 *
 * @param candidato      nome do candidato votado
 * @param posicao        posição atribuída ao candidato nessa categoria
 * @param pesoCategoria  peso da categoria, usado para ponderar a pontuação
 */
public record VotoCategoria(String candidato, Posicao posicao, double pesoCategoria) {

    /**
     * Valida que a posição foi informada.
     *
     * @throws NullPointerException se a posição for nula
     */
    public VotoCategoria {
        Objects.requireNonNull(posicao, "posicao");
    }

    /**
     * Cria um voto a partir da colocação numérica do candidato.
     *
     * @param candidato      nome do candidato votado
     * @param posicao        colocação atribuída ao candidato (1, 2 ou 3)
     * @param pesoCategoria  peso da categoria, usado para ponderar a pontuação
     * @throws IllegalArgumentException se a colocação não for 1, 2 ou 3
     */
    public VotoCategoria(String candidato, int posicao, double pesoCategoria) {
        this(candidato, Posicao.daColocacao(posicao), pesoCategoria);
    }
}
