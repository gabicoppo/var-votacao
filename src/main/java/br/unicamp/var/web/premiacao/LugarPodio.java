package br.unicamp.var.web.premiacao;

import java.util.List;

/**
 * Um degrau do pódio: uma colocação (1, 2 ou 3) e todos os candidatos que a dividem (história #30).
 *
 * @param colocacao  colocação do ranking, de 1 a 3
 * @param candidatos candidatos com essa colocação; mais de um quando há empate
 */
public record LugarPodio(int colocacao, List<CandidatoPodio> candidatos) {

    /** Guarda uma cópia imutável dos candidatos. */
    public LugarPodio {
        candidatos = List.copyOf(candidatos);
    }

    /**
     * Medalha do degrau, usada como classe CSS (ouro no centro, prata à esquerda, bronze à direita).
     *
     * @return {@code "ouro"}, {@code "prata"} ou {@code "bronze"}
     */
    public String medalha() {
        return switch (colocacao) {
            case 1 -> "ouro";
            case 2 -> "prata";
            default -> "bronze";
        };
    }
}
