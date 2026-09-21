package br.unicamp.var.apuracao2turno;

/**
 * Representa o resultado da apuração de uma votação majoritária:
 * ou um candidato foi eleito por maioria absoluta, ou o processo segue 
 * para segundo turno entre os dois candidatos mais votados.
 */
public sealed interface ResultadoApuracao {

    /**
     * Resultado em que um candidato foi eleito por maioria absoluta.
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
}