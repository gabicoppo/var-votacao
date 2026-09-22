package br.unicamp.var.top3;

/**
 * Representa o voto de uma categoria do Top 3: um candidato colocado numa
 * posição (1, 2 ou 3), com o peso da categoria que fez esse voto.
 *
 * @param candidato      nome do candidato votado
 * @param posicao        posição atribuída ao candidato nessa categoria (1, 2 ou 3)
 * @param pesoCategoria  peso da categoria, usado para ponderar a pontuação
 */
public record VotoCategoria(String candidato, int posicao, double pesoCategoria) {
}
