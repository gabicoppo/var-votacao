package br.unicamp.var.web.resultado;

/**
 * Resumo do 1º turno mostrado na página de resultado do 2º turno (história #21), para dar contexto.
 *
 * @param id       id da votação do 1º turno
 * @param pergunta pergunta do 1º turno
 * @param grafico  resultado do 1º turno (situação e barras)
 */
public record PrimeiroTurno(String id, String pergunta, GraficoResultado grafico) {
}
