package br.unicamp.var.web.resultado;

/**
 * O que a tela de resultado diz ao eleitor logado sobre o próprio voto (RNF-02), sem nunca dizer em
 * quem ele votou (RNF-01).
 */
public enum VotoDoEleitor {
    /** Quem consulta é o organizador: não há selo. */
    NAO_SE_APLICA,
    /** O eleitor votou: "Seu voto foi contado". */
    CONTADO,
    /** O eleitor está na lista de aptos, mas não votou. */
    NAO_VOTOU
}
