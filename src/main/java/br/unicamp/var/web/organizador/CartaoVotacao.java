package br.unicamp.var.web.organizador;

import br.unicamp.var.processo.PeriodoVotacao;
import br.unicamp.var.processo.Votacao;
import java.time.Instant;

/**
 * Card de uma votação no painel do organizador. Mostra só a participação (votantes / aptos),
 * nunca votos por opção (RNF-01).
 *
 * @param id           id da votação
 * @param pergunta     pergunta
 * @param status       "rascunho", "aberta", "encerrada" ou "apurada"
 * @param regra        nome curto da regra, como "Maioria absoluta"
 * @param votantes     quantos já votaram
 * @param aptos        tamanho da lista de aptos
 * @param inicio       início do período, ou {@code null} sem período
 * @param fim          fim do período, ou {@code null} sem período
 * @param turno        1 ou 2
 */
public record CartaoVotacao(String id, String pergunta, String status, String regra, int votantes, int aptos,
                            Instant inicio, Instant fim, int turno) {

    /**
     * Monta o card. Deve ser chamado com o monitor da votação ({@code synchronized (votacao)}).
     *
     * @param votacao votação lida
     * @return o card
     */
    static CartaoVotacao de(Votacao votacao) {
        PeriodoVotacao periodo = votacao.periodo().orElse(null);
        return new CartaoVotacao(votacao.id(), votacao.pergunta(), votacao.status(),
                RotulosVotacao.regraResumida(votacao.regra().descricao()), votacao.totalVotantes(),
                votacao.eleitorado().total(), periodo == null ? null : periodo.inicio(),
                periodo == null ? null : periodo.fim(), votacao.turno());
    }

    /**
     * Participação, de 0 a 1.
     *
     * @return votantes / aptos
     */
    public double participacao() {
        return aptos == 0 ? 0 : (double) votantes / aptos;
    }

    /**
     * Diz se a votação tem período definido.
     *
     * @return {@code true} se há início e fim
     */
    public boolean temPeriodo() {
        return inicio != null;
    }
}
