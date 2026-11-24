package br.unicamp.var.web.premiacao;

/**
 * Candidato num degrau do pódio (história #30).
 *
 * @param nome        nome do candidato
 * @param pontos      pontuação ponderada
 * @param pontosTexto pontuação para a tela, ex.: {@code "23,5 pts"}
 */
public record CandidatoPodio(String nome, double pontos, String pontosTexto) {
}
