package br.unicamp.var.web.organizador;

/**
 * Código de acesso gerado para um eleitor que ainda não tinha conta (história #24). Mostrado ao
 * organizador uma única vez, logo depois de criar a votação.
 *
 * @param eleitorId login do eleitor (o id na lista de aptos)
 * @param nome      nome do eleitor
 * @param codigo    senha inicial gerada
 */
public record AcessoEleitor(String eleitorId, String nome, String codigo) {
}
