package br.unicamp.var.web.premiacao;

/**
 * Código de acesso gerado para um eleitor que ainda não tinha conta (história #23). Mostrado ao
 * organizador uma única vez, logo depois de criar a premiação.
 *
 * @param eleitorId login do eleitor (o id na lista de aptos)
 * @param nome      nome do eleitor
 * @param codigo    senha inicial gerada
 */
public record AcessoPremiacao(String eleitorId, String nome, String codigo) {
}
