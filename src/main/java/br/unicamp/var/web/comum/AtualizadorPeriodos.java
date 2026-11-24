package br.unicamp.var.web.comum;

import br.unicamp.var.processo.Votacao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Abre e encerra as votações pelo período configurado (história #20), mesmo sem ninguém acessar a
 * votação. A cada 30 s chama {@link Votacao#atualizarPeloRelogio()} em todas as votações do
 * repositório, cada uma dentro de {@code synchronized (votacao)} (regra de
 * {@link RepositorioVotacoes}).
 */
@Component
public class AtualizadorPeriodos {

    private final RepositorioVotacoes votacoes;

    /**
     * Cria o atualizador.
     *
     * @param votacoes repositório de votações
     */
    public AtualizadorPeriodos(RepositorioVotacoes votacoes) {
        this.votacoes = votacoes;
    }

    /**
     * Aplica o relógio a todas as votações.
     */
    @Scheduled(fixedDelay = 30_000)
    public void atualizar() {
        for (Votacao votacao : votacoes.listar()) {
            synchronized (votacao) {
                votacao.atualizarPeloRelogio();
            }
        }
    }
}
