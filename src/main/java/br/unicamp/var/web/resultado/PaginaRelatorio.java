package br.unicamp.var.web.resultado;

import java.time.Instant;

/**
 * Ata de apuração imprimível (história #31).
 *
 * @param id        id da votação
 * @param pergunta  pergunta da votação
 * @param texto     texto de {@code RelatorioApuracao.formatar()}
 * @param codigo    código de verificação do texto ({@link CodigoVerificacao})
 * @param emitidoEm instante em que a página foi gerada
 */
public record PaginaRelatorio(String id, String pergunta, String texto, String codigo, Instant emitidoEm) {
}
