package br.unicamp.var.web.resultado;

/**
 * Resposta de {@code GET /votacoes/{id}/andamento.json} e {@code GET /premiacoes/{id}/andamento.json},
 * usada pela atualização ao vivo. Só contagens: nunca votos por opção (RNF-01).
 *
 * @param status   status atual, em minúsculas (ex.: {@code "aberta"})
 * @param votantes quantos já votaram
 * @param aptos    quantos podem votar
 */
public record AndamentoJson(String status, int votantes, int aptos) {
}
