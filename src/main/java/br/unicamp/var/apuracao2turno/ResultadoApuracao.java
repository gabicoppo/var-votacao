package br.unicamp.var.apuracao2turno;

/**
 * Representa o resultado da apuração de uma votação: um candidato foi eleito,
 * o processo segue para segundo turno entre os dois candidatos mais votados,
 * não houve vencedor ou o quórum mínimo não foi atingido.
 *
 * <p>{@link ApuracaoMajoritaria} produz apenas {@link Eleito} e
 * {@link SegundoTurno}; os demais casos vêm das regras de
 * {@code br.unicamp.var.apuracao}.</p>
 */
public sealed interface ResultadoApuracao {

    /**
     * Resultado em que um candidato foi eleito pela regra de apuração aplicada.
     *
     * @param candidato nome do candidato eleito
     */
    record Eleito(String candidato) implements ResultadoApuracao {
    }

    /**
     * Resultado em que nenhum candidato atingiu maioria absoluta e o 
     * processo segue para segundo turno entre os dois mais votados.
     *
     * @param primeiroColocado nome do candidato mais votado
     * @param segundoColocado  nome do segundo candidato mais votado
     */
    record SegundoTurno(String primeiroColocado, String segundoColocado) implements ResultadoApuracao {
    }

    /**
     * Resultado em que nenhum candidato venceu (por exemplo, empate, nenhum
     * voto válido ou fração exigida não alcançada).
     */
    record SemVencedor() implements ResultadoApuracao {
    }

    /**
     * Resultado em que o quórum mínimo de votantes não foi atingido; a
     * votação não tem vencedor.
     */
    record QuorumNaoAtingido() implements ResultadoApuracao {
    }
}
