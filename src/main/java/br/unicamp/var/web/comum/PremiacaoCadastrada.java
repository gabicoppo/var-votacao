package br.unicamp.var.web.comum;

import br.unicamp.var.top3.PremiacaoTop3;
import java.time.Instant;

/**
 * Premiação Top 3 guardada pela aplicação, com o id e o horário de cadastro que o domínio não tem.
 *
 * @param id        identificador gerado no cadastro (UUID)
 * @param premiacao premiação do domínio (já é thread-safe)
 * @param criadaEm  instante do cadastro, lido do {@link java.time.Clock} da aplicação
 */
public record PremiacaoCadastrada(String id, PremiacaoTop3 premiacao, Instant criadaEm) {
}
