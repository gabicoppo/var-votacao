package br.unicamp.var.processo;

import java.time.Instant;

/**
 * Período em que uma votação recebe votos (história #20), no intervalo semiaberto
 * {@code [inicio, fim)}: o instante de início já está dentro do período e o de fim já está fora.
 *
 * @param inicio instante em que a votação abre
 * @param fim    instante em que a votação encerra; deve ser depois de {@code inicio}
 */
public record PeriodoVotacao(Instant inicio, Instant fim) {

    /**
     * Valida o período.
     *
     * @throws IllegalArgumentException se algum instante for nulo ou se {@code fim} não for depois de {@code inicio}
     */
    public PeriodoVotacao {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("inicio e fim do periodo nao podem ser nulos");
        }
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("fim do periodo deve ser depois do inicio");
        }
    }

    /**
     * Diz se o instante está dentro do período.
     *
     * @param agora instante consultado
     * @return {@code true} se {@code inicio <= agora < fim}
     */
    public boolean contem(Instant agora) {
        return !naoComecou(agora) && !terminou(agora);
    }

    /**
     * Diz se o período ainda não começou.
     *
     * @param agora instante consultado
     * @return {@code true} se {@code agora < inicio}
     */
    public boolean naoComecou(Instant agora) {
        return agora.isBefore(inicio);
    }

    /**
     * Diz se o período já terminou.
     *
     * @param agora instante consultado
     * @return {@code true} se {@code agora >= fim}
     */
    public boolean terminou(Instant agora) {
        return !agora.isBefore(fim);
    }
}
