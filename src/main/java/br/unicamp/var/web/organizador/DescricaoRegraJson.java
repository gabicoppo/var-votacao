package br.unicamp.var.web.organizador;

/**
 * Descrição da regra escolhida no formulário, para o resumo ao vivo da nova votação. O texto é o
 * mesmo de {@code RegraApuracao.descricao()}.
 *
 * @param descricao texto da regra
 */
public record DescricaoRegraJson(String descricao) {
}
