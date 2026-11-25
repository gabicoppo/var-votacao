package br.unicamp.var.web.premiacao;

/**
 * Grupo de votantes com o peso formatado para a tela (história #23).
 *
 * @param nome      nome do grupo
 * @param pesoTexto peso, ex.: {@code "2"}, {@code "1,5"}
 */
public record GrupoPremiacao(String nome, String pesoTexto) {
}
