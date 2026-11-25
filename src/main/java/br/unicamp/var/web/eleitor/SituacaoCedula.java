package br.unicamp.var.web.eleitor;

/**
 * O que a cédula mostra para o eleitor apto.
 */
public enum SituacaoCedula {
    /** Aberta, dentro do período e sem voto do eleitor: mostra as opções. */
    PODE_VOTAR,
    /** O eleitor já votou (RNF-02): mostra o aviso e o link para verificar. */
    JA_VOTOU,
    /** Não está recebendo votos agora (rascunho, fora do período, encerrada ou apurada). */
    FECHADA
}
