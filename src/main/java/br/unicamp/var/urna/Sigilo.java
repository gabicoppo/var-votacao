package br.unicamp.var.urna;

/**
 * Sigilo do voto, escolhido na configuração da votação (história #22).
 */
public enum Sigilo {

    /**
     * Voto secreto (RNF-01): a urna guarda quem votou separado do conteúdo dos
     * votos, sem nenhuma ligação entre eles. É o caso das eleições do clube.
     */
    SECRETO,

    /**
     * Voto identificado: a urna guarda também a opção escolhida por cada eleitor,
     * consultável por {@link Urna#votoDe(String)}. É o caso das deliberações
     * internas com voto aberto.
     */
    IDENTIFICADO
}
