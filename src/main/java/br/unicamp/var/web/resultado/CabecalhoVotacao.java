package br.unicamp.var.web.resultado;

import java.time.Instant;

/**
 * Dados da votação comuns às telas de resultado e de andamento (histórias #19 a #22).
 *
 * @param id              id da votação
 * @param pergunta        pergunta
 * @param descricao       descrição, ou vazio
 * @param status          status em minúsculas
 * @param regra           descrição da regra de apuração
 * @param sigilo          sigilo para a tela, ex.: {@code "Secreto"}
 * @param turno           1 ou 2
 * @param turnoAnteriorId id do 1º turno, ou {@code null}
 * @param inicio          início do período, ou {@code null}
 * @param fim             fim do período, ou {@code null}
 */
public record CabecalhoVotacao(String id, String pergunta, String descricao, String status, String regra,
                               String sigilo, int turno, String turnoAnteriorId, Instant inicio, Instant fim) {
}
